package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;

@Entity
public class EntityInvalidIdTypeOnMethod {

    private CustomMethodClass customId;
    private List<String> listId;

    public EntityInvalidIdTypeOnMethod() {
    }

    // Invalid: Custom class return type
    @Id
    public CustomMethodClass getCustomId() {
        return customId;
    }

    // Invalid: Collection return type
    @Id
    public List<String> getListId() {
        return listId;
    }
}

class CustomMethodClass {
    private String value;
}

// Made with Bob
