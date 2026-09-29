package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;

/**
 * Valid: @AssociationOverride specifies joinTable only (no joinColumns).
 * Expected: no diagnostic.
 */
@Entity
@AssociationOverride(
    name      = "supervisor",
    joinTable = @JoinTable(name = "EMP_MGR")
)
public class ValidJoinTableOnly extends Person {
}
