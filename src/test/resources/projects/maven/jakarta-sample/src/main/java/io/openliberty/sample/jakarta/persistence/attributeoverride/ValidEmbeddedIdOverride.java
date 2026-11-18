package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * Valid: @AttributeOverride on an @EmbeddedId field with name="city" which exists in Address.
 * Expected: no diagnostic.
 */
@Entity
public class ValidEmbeddedIdOverride {
    @EmbeddedId
    @AttributeOverride(name = "city", column = @Column(name = "ID_CITY"))
    private Address address;
}
