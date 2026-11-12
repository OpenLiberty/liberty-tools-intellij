package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: @AssociationOverride on a property-based (getter) @Embedded accessor
 * with name="director" which does not exist in Department (only "manager" and "lead").
 * Expected: diagnostic InvalidAssociationOverrideName on the @AssociationOverride annotation.
 */
@Entity
public class InvalidPropertyBasedOverride {
    @Id
    private Long id;
    private Department dept;

    @Embedded
    @AssociationOverride(name = "director", joinColumns = @JoinColumn(name = "DIR_ID"))
    public Department getDept() {
        return dept;
    }
}
