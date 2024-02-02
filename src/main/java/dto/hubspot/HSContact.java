package dto.hubspot;

import lombok.Data;

import java.util.List;

@Data
public abstract class HSContact {
    List<Property> properties;
}
