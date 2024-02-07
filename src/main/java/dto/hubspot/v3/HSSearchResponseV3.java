package dto.hubspot.v3;

import lombok.Data;

import java.util.List;

@Data
public class HSSearchResponseV3 {
    private Integer total;
    private List<HSContactV3> results;
    private HSPaginationV3 paging;
}
