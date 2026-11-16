package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Invalid: AddressNotEmbeddable is NOT annotated with @Embeddable
// Diagnostic should fire on the @Embedded property accessor method
@Entity
public class EmbeddedMethodWithoutEmbeddable {

    @Id
    private Long id;

    private AddressNotEmbeddable address;

    @Embedded
    public AddressNotEmbeddable getAddress() {
        return address;
    }

    public void setAddress(AddressNotEmbeddable address) {
        this.address = address;
    }
}
