package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Root entity — direct @Entity parent of InheritanceOnNonRootEntity
@Entity
public class InheritanceEntityRoot {
    @Id
    private Long id;

    public InheritanceEntityRoot() {
    }
}
