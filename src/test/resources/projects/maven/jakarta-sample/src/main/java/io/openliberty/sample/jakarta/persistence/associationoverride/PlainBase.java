package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.ManyToOne;

/**
 * Plain superclass (NOT annotated with @MappedSuperclass) with relationship field: supervisor.
 * Used to verify that class-level @AssociationOverride only resolves against
 * associations inherited from a @MappedSuperclass.
 */
public abstract class PlainBase {
    @ManyToOne
    protected Employee supervisor;
}
