package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * Valid: dot-notation name="subDept.coordinator" — "subDept" in DepartmentWithTeam,
 * "coordinator" in SubTeam.
 * Expected: no diagnostic.
 */
@Entity
public class ValidDotNotationOverride {
    @Id
    private Long id;

    @Embedded
    @AssociationOverride(name = "subDept.coordinator", joinColumns = @JoinColumn(name = "COORD_ID"))
    private DepartmentWithTeam dept;
}
