package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

/**
 * Invalid: @AssociationOverrides container where one nested @AssociationOverride
 * specifies both joinColumns and joinTable.
 * Expected: one diagnostic AssociationOverrideBothJoinColumnsAndJoinTable on the
 * second nested entry; no diagnostic on the first (joinColumns only).
 */
@Entity
@AssociationOverrides({
    @AssociationOverride(name = "supervisor", joinColumns = @JoinColumn(name = "SUPER_ID")),
    @AssociationOverride(
        name        = "id",
        joinColumns = @JoinColumn(name = "MGR_ID"),
        joinTable   = @JoinTable(name = "EMP_MGR")
    )
})
public class InvalidContainerBothAttributes extends Person {
}
