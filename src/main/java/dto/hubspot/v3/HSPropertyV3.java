package dto.hubspot.v3;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) @AllArgsConstructor @NoArgsConstructor
public class HSPropertyV3 {
    private Instant createdate;
    private String email;
    private String firstname;
    private String hsObjectId;
    private String lastmodifieddate;
    private String lastname;
}
