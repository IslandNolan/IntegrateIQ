import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor @Getter @Setter
public class AuthInfo {
    private String awsBearerAuthToken = "";
    private String hubspotAuthToken = "";
    public String getAwsAuthHeader() {
        return String.format("Bearer %s",awsBearerAuthToken);
    }
    public String getHubspotAuthHeader() { return String.format("Bearer %s",hubspotAuthToken); }
}
