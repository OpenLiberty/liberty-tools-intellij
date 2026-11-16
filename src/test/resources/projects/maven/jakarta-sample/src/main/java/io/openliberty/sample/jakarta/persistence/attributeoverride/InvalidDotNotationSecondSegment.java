package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Invalid: dot-notation "zipcode.postcode" — first segment "zipcode" exists in
 * AddressWithZipcode, but second segment "postcode" does not exist in Zipcode.
 * Expected: diagnostic InvalidAttributeOverrideName on the @AttributeOverride annotation.
 */
@Entity
public class InvalidDotNotationSecondSegment {
    @Id
    private Long id;

    @Embedded
    @AttributeOverride(name = "zipcode.postcode", column = @Column(name = "ADDR_PC"))
    private AddressWithZipcode address;
}
