package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * Second-level MappedSuperclass extending Person, adding a teamLead association.
 * Used for depth-2 chain tests in @AssociationOverride validation.
 */
@MappedSuperclass
public abstract class NamedPerson extends Person {
    protected String name;

    @ManyToOne
    protected Employee teamLead;
}
