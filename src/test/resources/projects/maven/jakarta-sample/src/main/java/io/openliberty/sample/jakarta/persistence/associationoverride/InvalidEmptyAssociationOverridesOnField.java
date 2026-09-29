package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Invalid: @AssociationOverrides({}) empty container on an @Embedded field.
 * Expected: diagnostic AssociationOverridesEmptyContainer.
 */
@Entity
public class InvalidEmptyAssociationOverridesOnField {
    @Id
    private Long id;

    @Embedded
    @AssociationOverrides({})
    private Department dept;
}
