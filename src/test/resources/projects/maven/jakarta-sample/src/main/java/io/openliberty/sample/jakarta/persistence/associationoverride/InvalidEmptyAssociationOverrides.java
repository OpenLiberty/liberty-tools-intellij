package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Entity;

/**
 * Invalid: @AssociationOverrides container with no nested @AssociationOverride entries.
 * Expected: diagnostic AssociationOverridesEmptyContainer.
 */
@Entity
@AssociationOverrides({})
public class InvalidEmptyAssociationOverrides extends Person {
}
