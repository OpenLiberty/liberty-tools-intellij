package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Embeddable;

@Embeddable
public class AddressEmbeddable {
    private String street;
    private String city;
}
