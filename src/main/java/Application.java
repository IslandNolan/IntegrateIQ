import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.aws.SampleDataDto;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Objects;



/**
 * IntegrateIQ Tech Interview.
 * @author Nolan B,02/02/2024.
 */
@Log4j2
public class Application {

    //declare statically and register java time module for Instant.java (timestamps)
    public static final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    //Used to verify that all the contacts were updated successfully.
    public static final Instant timeStarted = Instant.now();
    public static void main(String[] args) {

        if(args.length==0) {

            //no arguments specified, just use bundled file from ~/resources/
            log.info("Using resources/settings.json");

            try(InputStream is = Objects.requireNonNull(Application.class.getResource("settings.json")).openStream()) {

                AuthInfo ai = om.readValue(is, AuthInfo.class);

                //process configurations in Application.java
                IntegrationRoutines integrationRoutines = IntegrationRoutines.builder().auth(ai)
                        .build();

                log.info("Using Hubspot access token: "+ai.getHubspotAuthHeader());

                //Fetch from AWS
                List<SampleDataDto> contacts = integrationRoutines.fetchAWSContacts();

                //Create or update by email
                int recordsModified = integrationRoutines.pushContacts(contacts);

                //Validate records were successfully created/updated.
                integrationRoutines.verify(recordsModified,0);

            } catch (IOException ex) {
                log.error(ex.getMessage());
            }

        } else log.info("CLI Arguments not supported, please modify the settings.json.old file to change credentials");
    }
}
