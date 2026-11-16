package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Valid: @AttributeOverride on a property-based (getter) @Embedded accessor
 * with name="city" which exists in Address.
 * Expected: no diagnostic.
 */
@Entity
public class ValidPropertyBasedOverride {
    @Id
    private Long id;
    private Address address;

    @Embedded
    @AttributeOverride(name = "city", column = @Column(name = "ADDR_CITY"))
    public Address getAddress() {
        return address;
    }
}
