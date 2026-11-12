package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * Valid: class-level @AssociationOverride with name="supervisor" which exists in Person.
 * Expected: no diagnostic.
 */
@Entity
@AssociationOverride(name = "supervisor", joinColumns = @JoinColumn(name = "SUPER_ID"))
public class ValidSuperclassOverride extends Person {
}
