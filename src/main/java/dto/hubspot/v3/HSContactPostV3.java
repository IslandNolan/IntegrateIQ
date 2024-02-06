package dto.hubspot.v3;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data @AllArgsConstructor
public class HSContactPostV3 {
    List<Wrapper> inputs = new ArrayList<>();
    public HSContactPostV3(ArrayList<HSPropertyV3> properties) {
        properties.forEach(contact -> inputs.add(new Wrapper(contact)));
    }
}
