package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

/**
 * Nested embeddable with a single association field: coordinator.
 * Used as the nested type in dot-notation @AssociationOverride validation tests.
 */
@Embeddable
public class SubTeam {
    @ManyToOne
    private Employee coordinator;
}
