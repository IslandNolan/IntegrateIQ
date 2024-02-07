package dto.hubspot.v3;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder @Data
public class HSSearchV3 {
    private Integer limit;
    private String after;
    private List<HSFilterGroupV3> filterGroups;
    private List<String> properties;
}

