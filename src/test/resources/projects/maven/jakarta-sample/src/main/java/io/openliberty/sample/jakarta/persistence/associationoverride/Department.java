package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

/**
 * Embeddable with two relationship fields: manager and lead.
 * Used as the target type for @AssociationOverride validation tests.
 */
@Embeddable
public class Department {
    @ManyToOne
    private Employee manager;

    @ManyToOne
    private Employee lead;
}
