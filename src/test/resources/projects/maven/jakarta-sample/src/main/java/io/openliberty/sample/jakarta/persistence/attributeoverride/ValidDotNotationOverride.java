package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Valid: dot-notation name="zipcode.zip" resolves correctly:
 * "zipcode" exists in AddressWithZipcode, "zip" exists in Zipcode.
 * Expected: no diagnostic.
 */
@Entity
public class ValidDotNotationOverride {
    @Id
    private Long id;

    @Embedded
    @AttributeOverride(name = "zipcode.zip", column = @Column(name = "ADDR_ZIP"))
    private AddressWithZipcode address;
}
