package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Map;

/**
 * Valid: name="value.city" on a Map @ElementCollection — "value." prefix present,
 * "city" exists in the value type Address.
 * Expected: no diagnostic.
 */
@Entity
public class ValidMapValueOverride {
    @Id
    private Long id;

    @ElementCollection
    @AttributeOverride(name = "value.city", column = @Column(name = "PROP_CITY"))
    private Map<String, Address> locations;
}
