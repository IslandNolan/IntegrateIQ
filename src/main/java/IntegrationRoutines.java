import dto.AuthInfo;
import dto.aws.SampleDataDto;
import dto.hubspot.HSContact;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

@Builder @AllArgsConstructor @Data
@NoArgsConstructor @Log4j2
public class IntegrationRoutines {

    private AuthInfo auth;

    @Builder.Default
    private OkHttpClient restClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .build();

    @Builder.Default
    private HashSet<SampleDataDto> contacts = new HashSet<>();

    public void fetchContacts() {

        Request contactsRequest = new Request.Builder()
                .addHeader("Authorization", getAuth().getAwsAuthHeader())
                .url(getAuth().getAwsResourcesUrl())
                .build();

        try (Response response = restClient.newCall(contactsRequest).execute()) {
            if (response.code() == 200) {

                //if 200 response, body will not be null
                assert response.body() != null;
                String body = response.body().string();

                //todo: add pagination if required..
                contacts.addAll(Arrays.stream(Application.om.readValue(body, SampleDataDto[].class)).toList());

            } else throw new IOException(response.code() + ": " + response.message());
        }
        catch (IOException e) { log.info("Request failed: "+e.getMessage()); }
    }

    public void mapToHubSpot(SampleDataDto contact) {

       //todo: map relevant data, decide to use legacy or v4.

    }

}
