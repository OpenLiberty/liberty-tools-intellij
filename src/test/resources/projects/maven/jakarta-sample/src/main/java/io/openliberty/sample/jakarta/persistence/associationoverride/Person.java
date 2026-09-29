package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * MappedSuperclass with relationship fields: id (scalar) and supervisor (association).
 * Used as the superclass target for @AssociationOverride validation tests.
 */
@MappedSuperclass
public abstract class Person {
    @Id
    protected Long id;

    @ManyToOne
    protected Employee supervisor;
}
