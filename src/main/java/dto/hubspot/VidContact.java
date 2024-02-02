package dto.hubspot;

//Exists just in case the email is null, that way we can search the contact info and try to merge it with another contact.
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @Builder(toBuilder = true) @EqualsAndHashCode(callSuper = true)
public class VidContact extends HSContact {
    private String vid;
}
