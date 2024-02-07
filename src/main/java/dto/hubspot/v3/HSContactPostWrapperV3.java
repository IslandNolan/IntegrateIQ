package dto.hubspot.v3;

import lombok.Data;

@Data
public class HSContactPostWrapperV3 {
    private Integer id;
    private HSPropertyV3 properties;
    public HSContactPostWrapperV3(HSPropertyV3 properties) {
        this.properties = properties;
    }
    public HSContactPostWrapperV3(HSPropertyV3 properties, Integer id) {
        this.properties = properties; 
        this.id = id;
    }
}
