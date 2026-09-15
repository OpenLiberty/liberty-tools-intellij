package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Invalid: AddressNotEmbeddable is NOT annotated with @Embeddable
// Diagnostic should fire on the @Embedded field
@Entity
public class EmbeddedWithoutEmbeddable {

    @Id
    private Long id;

    @Embedded
    private AddressNotEmbeddable address;
}
