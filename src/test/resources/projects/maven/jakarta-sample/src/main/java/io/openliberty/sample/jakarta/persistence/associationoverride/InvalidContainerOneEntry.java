package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: @AssociationOverrides container with "supervisor" (valid) and "mentor" (invalid — not in Person).
 * Expected: diagnostic fires on "mentor" entry only.
 */
@Entity
@AssociationOverrides({
    @AssociationOverride(name = "supervisor", joinColumns = @JoinColumn(name = "MGR_SUPER_ID")),
    @AssociationOverride(name = "mentor",     joinColumns = @JoinColumn(name = "MENTOR_ID"))
})
public class InvalidContainerOneEntry extends Person {
}
