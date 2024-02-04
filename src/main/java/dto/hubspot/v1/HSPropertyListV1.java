package dto.hubspot.v1;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class HSPropertyListV1 {
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String website;
    private String company;
}
