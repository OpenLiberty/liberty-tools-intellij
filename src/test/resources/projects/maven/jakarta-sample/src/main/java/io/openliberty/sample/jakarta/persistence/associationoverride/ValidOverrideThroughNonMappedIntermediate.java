package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * Valid: name="supervisor" resolves to Person.supervisor (@MappedSuperclass) through
 * the non-mapped intermediate class PlainPerson.
 * Expected: no diagnostic.
 */
@Entity
@AssociationOverride(name = "supervisor", joinColumns = @JoinColumn(name = "SUPER_ID"))
public class ValidOverrideThroughNonMappedIntermediate extends PlainPerson {
}
