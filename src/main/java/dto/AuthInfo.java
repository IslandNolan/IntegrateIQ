package dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @Getter @Setter
public class AuthInfo {
    private String awsResourcesUrl = "";
    private String awsBearerAuthToken = "";
    private String hubspotAuthToken = "";
    private String hubspotUrl = "";

    public String getAwsAuthHeader() {
        return "Bearer "+awsBearerAuthToken;
    }
}
