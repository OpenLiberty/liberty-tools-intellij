package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Invalid: name="zipcode" does not exist in Address (only "street" and "city").
 * Expected: diagnostic InvalidAttributeOverrideName on the @AttributeOverride annotation.
 */
@Entity
public class InvalidEmbeddableOverride {
    @Id
    private Long id;

    @Embedded
    @AttributeOverride(name = "zipcode", column = @Column(name = "ADDR_ZIP"))
    private Address address;
}
