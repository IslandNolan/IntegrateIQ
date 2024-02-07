package dto.hubspot.v3;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data @AllArgsConstructor
public class HSFilterGroupV3 {
    private List<HSFilterV3> filters = new ArrayList<>();
}
