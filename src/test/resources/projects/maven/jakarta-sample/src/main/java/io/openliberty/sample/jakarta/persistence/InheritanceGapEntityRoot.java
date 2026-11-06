package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Root entity — ancestor of InheritanceOnNonRootEntityWithGap via a non-entity abstract gap
@Entity
public class InheritanceGapEntityRoot {
    @Id
    private Long id;

    public InheritanceGapEntityRoot() {
    }
}
