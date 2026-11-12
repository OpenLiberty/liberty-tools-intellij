package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: name="director" does not exist in Department (only "manager" and "lead").
 * Expected: diagnostic InvalidAssociationOverrideName on the @AssociationOverride annotation.
 */
@Entity
public class InvalidEmbeddableOverride {
    @Id
    private Long id;

    @Embedded
    @AssociationOverride(name = "director", joinColumns = @JoinColumn(name = "DIR_ID"))
    private Department dept;
}
