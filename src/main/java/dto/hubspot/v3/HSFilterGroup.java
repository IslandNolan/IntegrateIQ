package dto.hubspot.v3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data @AllArgsConstructor
public class HSFilterGroup {
    private List<HSFilter> filters = new ArrayList<>();
}
