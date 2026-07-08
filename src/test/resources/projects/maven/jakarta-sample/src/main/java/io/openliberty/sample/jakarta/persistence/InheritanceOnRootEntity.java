package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

// Valid: @Entity + @Inheritance on the root class — no entity ancestor
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class InheritanceOnRootEntity {
    @Id
    private Long id;

    public InheritanceOnRootEntity() {
    }
}
