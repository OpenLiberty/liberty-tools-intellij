package io.openliberty.sample.jakarta.persistence;

// Plain class with no @Embeddable annotation — used to test @Embedded validation
public class AddressNotEmbeddable {
    private String street;
    private String city;
}
