package dto.hubspot;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @Builder(toBuilder = true) @EqualsAndHashCode(callSuper = true)
public class EmailContact extends HSContact {
    private String email;
}
