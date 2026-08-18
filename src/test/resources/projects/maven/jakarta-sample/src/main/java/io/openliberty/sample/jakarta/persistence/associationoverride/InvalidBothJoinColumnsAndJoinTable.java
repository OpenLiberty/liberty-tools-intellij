package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

/**
 * Invalid: @AssociationOverride specifies both joinColumns and joinTable.
 * Expected: diagnostic AssociationOverrideBothJoinColumnsAndJoinTable.
 */
@Entity
@AssociationOverride(
    name        = "supervisor",
    joinColumns = @JoinColumn(name = "MGR_ID"),
    joinTable   = @JoinTable(name = "EMP_MGR")
)
public class InvalidBothJoinColumnsAndJoinTable extends Person {
}
