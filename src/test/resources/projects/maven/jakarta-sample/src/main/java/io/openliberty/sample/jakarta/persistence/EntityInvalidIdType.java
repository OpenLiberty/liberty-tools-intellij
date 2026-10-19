package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
public class EntityInvalidIdType {

    // Invalid: Custom class type
    @Id
    private CustomClass customId;

    // Invalid: Collection type
    @Id
    private List<String> listId;

    // Invalid: Array type
    @Id
    private int[] arrayId;

    // Invalid: String array
    @Id
    private String[] stringArrayId;

    // Invalid: UUID (not in Jakarta Persistence 3.0 spec)
    @Id
    private UUID uuidId;

    @Id
    private Set<String> setId;

    @Id
    private Map<String, String> mapId;

    public EntityInvalidIdType() {
    }
}

class CustomClass {
    private String value;
}

