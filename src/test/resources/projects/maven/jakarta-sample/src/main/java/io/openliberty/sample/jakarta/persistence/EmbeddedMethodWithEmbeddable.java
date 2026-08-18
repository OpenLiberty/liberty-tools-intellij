package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Valid: AddressEmbeddable is annotated with @Embeddable
// No diagnostic expected on the property accessor
@Entity
public class EmbeddedMethodWithEmbeddable {

    @Id
    private Long id;

    private AddressEmbeddable address;

    @Embedded
    public AddressEmbeddable getAddress() {
        return address;
    }

    public void setAddress(AddressEmbeddable address) {
        this.address = address;
    }
}
