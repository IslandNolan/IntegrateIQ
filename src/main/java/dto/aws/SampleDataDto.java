package dto.aws;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
@Data @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SampleDataDto {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String gender;
    private String phoneNumber;
}
