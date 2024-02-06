package dto.hubspot.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @AllArgsConstructor @Builder
public class HSFilter {
    private String propertyName;
    private String operator;
    private List<String> values;
    private String value;
    private String highValue;
}
