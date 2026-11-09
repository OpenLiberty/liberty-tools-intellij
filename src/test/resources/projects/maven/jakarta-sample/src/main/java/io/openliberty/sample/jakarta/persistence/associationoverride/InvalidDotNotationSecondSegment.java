package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: dot-notation "subDept.owner" — first segment "subDept" exists in
 * DepartmentWithTeam, but second segment "owner" does not exist in SubTeam.
 * Expected: diagnostic InvalidAssociationOverrideName on the @AssociationOverride annotation.
 */
@Entity
public class InvalidDotNotationSecondSegment {
    @Id
    private Long id;

    @Embedded
    @AssociationOverride(name = "subDept.owner", joinColumns = @JoinColumn(name = "OWNER_ID"))
    private DepartmentWithTeam dept;
}
