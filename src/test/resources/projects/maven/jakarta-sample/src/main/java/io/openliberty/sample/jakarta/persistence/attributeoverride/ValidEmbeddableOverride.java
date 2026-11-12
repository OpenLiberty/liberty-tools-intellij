package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Valid: @AttributeOverride on @Embedded field with name="city" which exists in Address.
 * Expected: no diagnostic.
 */
@Entity
public class ValidEmbeddableOverride {
    @Id
    private Long id;

    @Embedded
    @AttributeOverride(name = "city", column = @Column(name = "ADDR_CITY"))
    private Address address;
}
