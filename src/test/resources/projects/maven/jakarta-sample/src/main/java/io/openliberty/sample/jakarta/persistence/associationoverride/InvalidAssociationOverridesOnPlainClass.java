package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: @AssociationOverrides (container) applied to a plain class that is neither
 * @Entity, @MappedSuperclass, nor @Embeddable.
 * Expected: diagnostic AssociationOverrideOnInvalidTarget.
 */
@AssociationOverrides({
    @AssociationOverride(name = "address", joinColumns = @JoinColumn(name = "ADDR_ID")),
    @AssociationOverride(name = "contact", joinColumns = @JoinColumn(name = "CONTACT_ID"))
})
public class InvalidAssociationOverridesOnPlainClass {
    private String name;
}
