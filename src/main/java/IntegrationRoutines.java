import dto.aws.SampleDataDto;
import dto.hubspot.v1.HSPostContactsV1;
import dto.hubspot.v1.HSPostPropertyListV1;
import dto.hubspot.v3.*;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.*;

@Builder @Data @Log4j2
public class IntegrationRoutines {

    private static final String AWS_DATA_URL = "https://l0hefgbbla.execute-api.us-east-1.amazonaws.com/prod/contacts";
    private static final String HS_URL_BASE = "https://api.hubapi.com/";
    private static final String HS_CONTACTS_V3 = HS_URL_BASE+"crm/v3/objects/contacts";
    private static final String HS_CONTACTS_BATCH_V3_CREATE = HS_CONTACTS_V3+"/batch/create";
    private static final String HS_CONTACTS_BATCH_V3_UPDATE = HS_CONTACTS_V3+"/batch/update";
    private static final String HS_CONTACTS_BATCH_V3_SEARCH = HS_CONTACTS_V3+"/search";
    private static final String HS_CONTACTS_BATCH_V1 = HS_URL_BASE+"contacts/v1/contact/batch/";
    private static final String AUTHORIZATION = "Authorization";
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");

    @NonNull private AuthInfo auth;

    @Builder.Default
    private OkHttpClient restClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .build();
    //Used for fetching existing contacts from HS
    @Builder.Default private ArrayList<HSContactV3> hsContacts = new ArrayList<>();

    public List<SampleDataDto> fetchAWSContacts() {

        log.info("Retrieving contacts from AWS");
        ArrayList<SampleDataDto> contacts = new ArrayList<>();

        Request contactsRequest = new Request.Builder()
                .addHeader(AUTHORIZATION, getAuth().getAwsAuthHeader())
                .url(AWS_DATA_URL)
                .build();

        try (Response response = restClient.newCall(contactsRequest).execute()) {
            if (response.code() == 200) {

                //if 200 response, body will not be null
                assert response.body() != null;
                String body = response.body().string();

                contacts = new ArrayList<>(Arrays.stream(Application.om.readValue(body, SampleDataDto[].class)).toList());
                log.info(String.format("Retrieved %s AWS contact(s)", contacts.size()));

            } else throw new IOException(response.code() + ": " + response.message());
        } catch (IOException e) { log.info("Request failed -> "+e.getMessage()); }

        return contacts;
    }
    public static HSPostPropertyListV1 map(SampleDataDto contact) {
        return HSPostPropertyListV1.builder()
                .firstname(contact.getFirstName())
                .lastname(contact.getLastName())
                .email(contact.getEmail())
                .phone(contact.getPhoneNumber())
                .build();
    }
    public static HSPropertyV3 mapV3(SampleDataDto contact) {
        return HSPropertyV3.builder()
                .firstname(contact.getFirstName())
                .lastname(contact.getLastName())
                .phone(contact.getPhoneNumber())
                .email(contact.getEmail())
                .build();
    }
    public int pushContacts(List<SampleDataDto> contacts) {

        log.info(String.format("Posting %s contacts to Hubspot",contacts.size()));

        AtomicInteger numberOfContactsModified = new AtomicInteger(0);
        HashSet<SampleDataDto> altContacts = new HashSet<>();
        //Attempt to batch create/update with the v1 endpoints. If any batch fails send one at a time until
        // we find the failing one, then add it to an error log

        Request.Builder rs = new Request.Builder()
                .addHeader(AUTHORIZATION,getAuth().getHubspotAuthHeader())
                .url(HS_CONTACTS_BATCH_V1);

        //Split this into sets of 50 contacts. Small batches better in case one request fails.
        ListUtils.partition(contacts.stream().filter(contact -> {
                    if(Objects.isNull(contact.getEmail())) {
                        //if the email does not exist, then we must upload this contact individually
                        altContacts.add(contact);
                        return false;
                    }
                    return true;
                })
                .map(IntegrationRoutines::map)
                .map(HSPostContactsV1::new)
                .toList(),50)
                .forEach(mappedContacts -> {
                    try {
                        String requestString = Application.om.writeValueAsString(mappedContacts);

                        //Clone above request with .newBuilder, then add the current body and send request.
                        Request postContactsRequest = rs.build().newBuilder()
                                .method("POST", RequestBody.create(requestString, MEDIA_TYPE))
                                .build();

                        Response postContactsResponse = restClient.newCall(postContactsRequest).execute();
                        switch (postContactsResponse.code()) {
                            case 202 -> numberOfContactsModified.updateAndGet(v -> v + mappedContacts.size());
                            default -> log.info("Status: " + postContactsResponse.code() + ": " + postContactsResponse.message() + " -> " + requestString);
                        }
                    } catch (Exception ex) { log.error("Something went wrong while posting contacts to "+HS_CONTACTS_BATCH_V1); }
                });

        log.info(String.format("Posted %s regular contacts",contacts.size()-altContacts.size()));

        try {
            //This was created to process contacts returned from AWS that do not contain an email.
            numberOfContactsModified.addAndGet(processAlternativeContacts(altContacts));
        } catch (Exception ex) { log.error("Something went wrong while posting alternative contacts to "+HS_CONTACTS_BATCH_V3_CREATE); }

        return numberOfContactsModified.get();
    }
    private int processAlternativeContacts(HashSet<SampleDataDto> altContacts) throws Exception {

        AtomicInteger altContactsModified = new AtomicInteger(0);

        if (!altContacts.isEmpty()) {
            log.info(String.format("Discovered %s contacts with no email. Using Phone Number", altContacts.size()));
            HashMap<String, Integer> existingContacts = new HashMap<>(); //<phone #, hs_id>

            //phone is not included in response by default, this will act as our way to identify existing contacts
            List<HSFilterGroupV3> phoneNumberFilter = List.of(new HSFilterGroupV3(List.of(HSFilterV3.builder()
                    .propertyName("phone")
                    .operator("IN")
                    .values(altContacts.stream().map(SampleDataDto::getPhoneNumber).toList())
                    .build())
            ));
            HSSearchV3 searchv3 = HSSearchV3.builder()
                    .limit(altContacts.size())
                    .filterGroups(phoneNumberFilter)
                    .properties(List.of("phone", "email", "firstname", "lastname", "email"))
                    .build();

            Request searchRequest = new Request.Builder()
                    .addHeader(AUTHORIZATION, getAuth().getHubspotAuthHeader())
                    .url(HS_CONTACTS_BATCH_V3_SEARCH)
                    .method("POST", RequestBody.create(Application.om.writeValueAsString(searchv3), MEDIA_TYPE))
                    .build();

            Response searchResponse = restClient.newCall(searchRequest).execute();

            //make request, read into object.
            HSSearchResponseV3 searchResults = Application.om.readValue(searchResponse.body().string(), HSSearchResponseV3.class);
            searchResults.getResults().forEach(contact -> existingContacts.put(contact.getProperties().getPhone(), contact.getId()));

            //create all the ones that do not exist, update the rest.
            ArrayList<HSPropertyV3> create = new ArrayList<>();
            ArrayList<HSPropertyV3> update = new ArrayList<>();

            altContacts.forEach(contact -> {
                if (!existingContacts.containsKey(contact.getPhoneNumber())) {
                    create.add(mapV3(contact));
                } else update.add(mapV3(contact));
            });

            if (!create.isEmpty()) {
                String createBody = Application.om.writeValueAsString(new HSContactPostV3(create));
                Request createRequest = new Request.Builder()
                        .addHeader(AUTHORIZATION, getAuth().getHubspotAuthHeader())
                        .url(HS_CONTACTS_BATCH_V3_CREATE)
                        .method("POST", RequestBody.create(createBody, MEDIA_TYPE))
                        .build();

                Response createResponse = restClient.newCall(createRequest).execute();
                switch (createResponse.code()) {
                    case 201 -> {
                        altContactsModified.addAndGet(create.size());
                        log.info(String.format("Created %s alternate contacts", create.size()));
                    }
                }
            }
            if (!update.isEmpty()) {

                HSContactPostV3 bodyContent = new HSContactPostV3(update);
                bodyContent.getInputs().forEach(element -> element.setId(existingContacts.get(element.getProperties().getPhone())));

                String updateBody = Application.om.writeValueAsString(bodyContent);

                Request updateRequest = new Request.Builder()
                        .addHeader(AUTHORIZATION, getAuth().getHubspotAuthHeader())
                        .url(HS_CONTACTS_BATCH_V3_UPDATE)
                        .method("POST", RequestBody.create(updateBody, MEDIA_TYPE))
                        .build();

                Response updateResponse = restClient.newCall(updateRequest).execute();
                switch (updateResponse.code()) {
                    case 200 -> {
                        altContactsModified.addAndGet(update.size());
                        log.info(String.format("Updated %s alternate contacts", update.size()));
                    }
                }
            }
        }

        return altContactsModified.get();
    }

    /**
     * Verify # of records modified

     * Works by recording the timestamp when the application starts,
     * then passing the epoch second to Hubspot to check how many records have been updated.
     */
    public void verify(int numberOfContactsModified, int retries) {

        try {
            List<HSFilterGroupV3> timestampFilter = List.of(new HSFilterGroupV3(List.of(HSFilterV3.builder()
                    .value(String.valueOf(Application.timeStarted.getEpochSecond()))
                    .operator("GT")
                    .propertyName("lastmodifieddate")
                    .build()
            )));

            HSSearchV3 searchv3 = HSSearchV3.builder()
                    //just limit to one, all we care about is the 'total' value since this is restricted by timestamp.
                    .limit(1)
                    .filterGroups(timestampFilter)
                    .properties(List.of("phone","email","firstname","lastname","email"))
                    .build();

            Request searchRequest = new Request.Builder()
                    .addHeader(AUTHORIZATION, getAuth().getHubspotAuthHeader())
                    .url(HS_CONTACTS_BATCH_V3_SEARCH)
                    .method("POST",RequestBody.create(Application.om.writeValueAsString(searchv3),MEDIA_TYPE))
                    .build();

            HSSearchResponseV3 response = Application.om.readValue(restClient.newCall(searchRequest).execute().body().string(), HSSearchResponseV3.class);
            if(response.getTotal().equals(numberOfContactsModified)) {
                log.info(String.format("[VERIFY] %s contacts updated/created",numberOfContactsModified));
            }
            else {
                if(retries+1>4) {
                    log.error("[VERIFY] Unable to verify modified contacts after 4 retries.. ");
                } else {
                    log.info(String.format("[VERIFY] Hubspot still processing.. waiting 15 seconds, retry #%s",retries+1));
                    Thread.sleep(10000);
                    verify(numberOfContactsModified,retries+1);
                }
            }
        }
        catch (Exception ignore) {}
    }
}
