import dto.aws.SampleDataDto;
import dto.hubspot.v1.HSPostContactsV1;
import dto.hubspot.v1.HSPropertyListV1;
import dto.hubspot.v3.HSContactV3;
import dto.hubspot.v3.HSGetContactsV3;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.*;

@Builder @Data @Log4j2
public class IntegrationRoutines {

    private static final String AWS_DATA_URL = "https://l0hefgbbla.execute-api.us-east-1.amazonaws.com/prod/contacts";
    private static final String HS_URL_BASE = "https://api.hubapi.com/";
    private static final String HS_CONTACTS_BATCH_V3 = "crm/v3/objects/contacts";
    private static final String HS_CONTACTS_BATCH_V1 = "contacts/v1/contact/batch/";
    private static final String AUTHORIZATION = "Authorization";

    @NonNull private AuthInfo auth;

    @Builder.Default
    private OkHttpClient restClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .build();

    @Builder.Default private List<SampleDataDto> contacts = new ArrayList<>();
    @Builder.Default private ArrayList<HSContactV3> hsContacts = new ArrayList<>();
    @Builder.Default private HashSet<String> emails = new HashSet<>();
    @Builder.Default private EnumMap<HubspotMode,ArrayList<HSPropertyListV1>> postMappings = new EnumMap<>(HubspotMode.class);

    public void fetchAWSContacts() {

        log.info("Retrieving contacts.. ");

        Request contactsRequest = new Request.Builder()
                .addHeader(AUTHORIZATION, getAuth().getAwsAuthHeader())
                .url(AWS_DATA_URL)
                .build();

        try (Response response = restClient.newCall(contactsRequest).execute()) {
            if (response.code() == 200) {

                //if 200 response, body will not be null
                assert response.body() != null;
                String body = response.body().string();

                //todo: add pagination if required..
                contacts.addAll(Arrays.stream(Application.om.readValue(body, SampleDataDto[].class)).toList());
                log.info(String.format("Fetched %s AWS contact(s)", getContacts().size()));


            } else throw new IOException(response.code() + ": " + response.message());
        }
        catch (IOException e) { log.info("Request failed -> "+e.getMessage()); }
    }

    public void getHubspotContacts(String cursor, int retries) {

        //if max retries, then just return and do nothing
        if(retries>3) { log.error("Maximum number of retries exceeded"); return; }

        //Fetch all emails, then decide to include in UPDATE, or CREATE.
        HttpUrl.Builder urlBuild = Objects.requireNonNull(HttpUrl.parse(HS_URL_BASE + HS_CONTACTS_BATCH_V3)).newBuilder();
        if(Objects.nonNull(cursor)) { urlBuild.addQueryParameter("after",cursor); }

        Request contactsRequest = new Request.Builder()
                .addHeader(AUTHORIZATION, getAuth().getHubspotAuthHeader())
                .method("GET",null)
                .url(
                    urlBuild.addQueryParameter("limit","100").build()
                )
                .build();

        try (Response response = restClient.newCall(contactsRequest).execute()) {

            //careful of rate limiting for get request (100 Burst/10 seconds)
            switch(response.code()) {
                case 200 -> {
                    assert response.body() != null;
                    String body = response.body().string();

                    HSGetContactsV3 contactList = Application.om.readValue(body, HSGetContactsV3.class);
                    hsContacts.addAll(contactList.getResults());

                    log.info(String.format("Fetched %s HS contact(s)", hsContacts.size()));

                    if(Objects.nonNull(contactList.getPaging())) {
                        getHubspotContacts(contactList.getPaging().getNext().getAfter(),retries);
                    }
                }
                case 429 -> {
                    //Rate limited by hubspot
                    log.info("HS Rate Limit, waiting 10 seconds");
                    Thread.sleep(10000);
                    getHubspotContacts(cursor,retries+1);
                }
                default -> log.warn("Unrecognized response status: "+response.code());
            }
        }
        catch (IOException e) {  log.error("Request failed -> "+e.getMessage()); }
        catch (InterruptedException ignored) { }
    }

    public static HSPropertyListV1 map(SampleDataDto contact) {
        return HSPropertyListV1.builder()
                .firstname(contact.getFirstName())
                .lastname(contact.getLastName())
                .email(contact.getEmail())
                .phone(contact.getPhoneNumber())
                .build();
    }
    public void pushContacts() {

        AtomicInteger contactsWNoEmail = new AtomicInteger(0);
        //Attempt to batch create/update with the v1 endpoints. If any batch fails send one at a time until
        // we find the failing one, then add it to an error log

        Request.Builder rs = new Request.Builder()
                .addHeader(AUTHORIZATION,getAuth().getHubspotAuthHeader())
                .url(HS_URL_BASE+HS_CONTACTS_BATCH_V1);

        ListUtils.partition(contacts,50).forEach(propertyList -> {

            List<HSPostContactsV1> requestBody = propertyList.stream()
                    .filter(contact -> {
                        if(Objects.isNull(contact.getEmail())) { contactsWNoEmail.incrementAndGet(); return false; }
                        return true;
                    })
                    .map(contact -> new HSPostContactsV1(map(contact))).toList();

            try {
                String requestString = Application.om.writeValueAsString(requestBody);
                Request.Builder newRs = rs.build().newBuilder()
                        .method("POST", RequestBody.create(requestString,MediaType.get("application/json")));

                Response response = restClient.newCall(newRs.build()).execute();
                switch(response.code()) {
                    default -> log.info("Status: "+response.code()+": "+ response.message()+" -> "+requestString);
                }
            } catch (Exception ignored) {}
        });

        log.info(String.format("Contacts with no email: %s",contactsWNoEmail.get()));
    }

    public void verify(Instant applicationStartTime) {
        //verify emails that were sent for legacy endpoint
        //verify vid by singular creation after endpoint returns it.
        //(investigate timestamp capabilities)
    }
}
