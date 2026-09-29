package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Invalid: dot-notation "location.zip" — first segment "location" does not exist
 * in AddressWithZipcode at all.
 * Expected: diagnostic InvalidAttributeOverrideName on the @AttributeOverride annotation.
 */
@Entity
public class InvalidDotNotationFirstSegment {
    @Id
    private Long id;

    @Embedded
    @AttributeOverride(name = "location.zip", column = @Column(name = "LOC_ZIP"))
    private AddressWithZipcode address;
}
