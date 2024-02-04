package dto.hubspot.v3;

import lombok.Data;

import java.time.Instant;

@Data
public class HSContactV3 {
    private Integer id;
    private HSPropertyV3 properties;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean archived;
}
