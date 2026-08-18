package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Embeddable
class AddressEmbeddable {
    private String street;
    private String city;
}

// Valid: AddressEmbeddable is annotated with @Embeddable — no diagnostic expected
@Entity
public class EmbeddedWithEmbeddable {

    @Id
    private Long id;

    @Embedded
    private AddressEmbeddable address;
}
