package dto.hubspot.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data @NoArgsConstructor
public class HSPostContactsV1 {

    private String email = null;
    private List<HSProperty> properties = new ArrayList<>();
    public HSPostContactsV1 addProperty(HSProperty property) {
        properties.add(property);
        return this;
    }
    public HSPostContactsV1(HSPostPropertyListV1 list) {

        properties = new ArrayList<>(List.of(
                new HSProperty("firstname", list.getFirstname()),
                new HSProperty("lastname", list.getLastname()),
                new HSProperty("phone", list.getPhone()),
                new HSProperty("wesbite", list.getWebsite()),
                new HSProperty("company", list.getCompany())
        ));

        //email always present. You cannot create contacts in batch by specifying a new vid,
        this.email = list.getEmail();

        //remove blank properties. not allows by hubspot api.
        properties.removeIf(hsProperty -> Objects.isNull(hsProperty.value));
    }
    @Data @AllArgsConstructor
    public static class HSProperty {
        private String property;
        private String value;
    }
}
