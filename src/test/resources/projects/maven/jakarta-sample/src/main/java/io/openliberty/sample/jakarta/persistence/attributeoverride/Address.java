package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.Embeddable;

/**
 * Embeddable with two fields: street and city.
 * Used as the target type for @AttributeOverride validation tests.
 */
@Embeddable
public class Address {
    private String street;
    private String city;
}
