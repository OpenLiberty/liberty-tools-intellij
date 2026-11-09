package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: dot-notation "division.coordinator" — first segment "division" does not exist
 * in DepartmentWithTeam at all.
 * Expected: diagnostic InvalidAssociationOverrideName on the @AssociationOverride annotation.
 */
@Entity
public class InvalidDotNotationFirstSegment {
    @Id
    private Long id;

    @Embedded
    @AssociationOverride(name = "division.coordinator", joinColumns = @JoinColumn(name = "COORD_ID"))
    private DepartmentWithTeam dept;
}
