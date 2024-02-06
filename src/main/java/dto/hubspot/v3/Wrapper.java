package dto.hubspot.v3;

import lombok.Data;

@Data
public class Wrapper {
    private Integer id;
    private HSPropertyV3 properties;
    public Wrapper(HSPropertyV3 properties) {
        this.properties = properties;
    }
    public Wrapper(HSPropertyV3 properties, Integer id) {
        this.properties = properties; 
        this.id = id;
    }
}
