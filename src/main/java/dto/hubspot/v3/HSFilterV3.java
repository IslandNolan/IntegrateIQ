package dto.hubspot.v3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @AllArgsConstructor @Builder
public class HSFilterV3 {
    private String propertyName;
    private String operator;
    private List<String> values;
    private String value;
    private String highValue;
}
