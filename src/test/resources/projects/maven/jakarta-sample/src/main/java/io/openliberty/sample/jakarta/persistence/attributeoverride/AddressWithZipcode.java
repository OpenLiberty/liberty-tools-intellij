package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

/**
 * Embeddable with a nested embeddable field (zipcode).
 * Fields: street, city, zipcode (of type Zipcode).
 * Used for dot-notation override validation tests.
 */
@Embeddable
public class AddressWithZipcode {
    private String street;
    private String city;

    @Embedded
    private Zipcode zipcode;
}
