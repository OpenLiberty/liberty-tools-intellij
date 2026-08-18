package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: @AssociationOverride applied to a plain class that is neither
 * @Entity, @MappedSuperclass, nor @Embeddable.
 * Expected: diagnostic AssociationOverrideOnInvalidTarget.
 */
@AssociationOverride(name = "address", joinColumns = @JoinColumn(name = "ADDR_ID"))
public class InvalidAssociationOverrideOnPlainClass {
    private String name;
}
