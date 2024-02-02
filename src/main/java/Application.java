import com.fasterxml.jackson.databind.ObjectMapper;
import dto.AuthInfo;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;



/**
 * IntegrateIQ Tech Interview.
 * @author Nolan B,02/02/2024.
 */
@Log4j2
public class Application {

    //process configurations in Application.java
    private static IntegrationRoutines integrationRoutines;
    public static final ObjectMapper om = new ObjectMapper();
    public static void main(String[] args) {

        if(args.length==0) {

            //no arguments specified, just use bundled file from ~/resources/
            log.info("Using bundled settings.json");

            try(InputStream is = Objects.requireNonNull(Application.class.getResource("settings.json")).openStream()) {

                AuthInfo ai = om.readValue(is, AuthInfo.class);
                integrationRoutines = IntegrationRoutines.builder()
                        .auth(ai)
                        .build();


                //Fetch
                log.info("Getting Contacts.. ");
                integrationRoutines.fetchContacts();
                log.info(String.format("Fetched %s contact(s)",integrationRoutines.getContacts().size()));

                //Create or update by email

                //Validate records were successfully created/updated.


            } catch (IOException ex) {
                log.error(ex.getMessage());
            }

        } else log.info("CLI Arguments not supported, please modify the settings.json file to change credentials");
    }
}
