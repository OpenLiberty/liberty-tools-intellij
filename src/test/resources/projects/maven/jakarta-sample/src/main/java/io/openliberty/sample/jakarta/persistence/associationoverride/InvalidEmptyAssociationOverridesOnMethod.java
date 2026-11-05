package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Invalid: @AssociationOverrides({}) empty container on a property-based
 * (getter) @Embedded accessor.
 * Expected: diagnostic AssociationOverridesEmptyContainer.
 */
@Entity
public class InvalidEmptyAssociationOverridesOnMethod {
    @Id
    private Long id;
    private Department dept;

    @Embedded
    @AssociationOverrides({})
    public Department getDept() {
        return dept;
    }
}
