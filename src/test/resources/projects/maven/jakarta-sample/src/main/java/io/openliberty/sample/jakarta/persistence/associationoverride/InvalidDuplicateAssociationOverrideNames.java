package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: @AssociationOverrides container with two entries that share the same
 * name "supervisor". The second entry is a duplicate.
 * Expected: diagnostic AssociationOverridesDuplicateName on the second entry.
 */
@Entity
@AssociationOverrides({
    @AssociationOverride(name = "supervisor", joinColumns = @JoinColumn(name = "SUP_ID")),
    @AssociationOverride(name = "supervisor", joinColumns = @JoinColumn(name = "SUP2_ID"))
})
public class InvalidDuplicateAssociationOverrideNames extends Person {
}
