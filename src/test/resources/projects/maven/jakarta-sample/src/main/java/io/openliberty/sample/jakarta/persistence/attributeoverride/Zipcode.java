package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.Embeddable;

/**
 * Embeddable used as a nested type inside AddressWithZipcode.
 * Fields: zip, plusFour.
 */
@Embeddable
public class Zipcode {
    private String zip;
    private String plusFour;
}
