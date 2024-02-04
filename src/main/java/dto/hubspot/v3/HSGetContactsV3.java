package dto.hubspot.v3;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HSGetContactsV3 {
    List<HSContactV3> results;
    HSPaginationV3 paging;
}

