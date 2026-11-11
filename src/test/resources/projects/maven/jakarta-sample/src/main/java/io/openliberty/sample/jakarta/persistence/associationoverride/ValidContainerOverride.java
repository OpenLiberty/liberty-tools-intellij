package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * Valid: @AssociationOverrides container — both "supervisor" and "id" exist in Person.
 * Expected: no diagnostic.
 */
@Entity
@AssociationOverrides({
    @AssociationOverride(name = "supervisor", joinColumns = @JoinColumn(name = "MGR_SUPER_ID")),
    @AssociationOverride(name = "id",         joinColumns = @JoinColumn(name = "MGR_ID"))
})
public class ValidContainerOverride extends Person {
}
