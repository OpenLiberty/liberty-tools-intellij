package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: name="mentor" does not exist in Person (only "supervisor" and "id").
 * Expected: diagnostic InvalidAssociationOverrideName on the class-level @AssociationOverride.
 */
@Entity
@AssociationOverride(name = "mentor", joinColumns = @JoinColumn(name = "MENTOR_ID"))
public class InvalidSuperclassOverride extends Person {
}
