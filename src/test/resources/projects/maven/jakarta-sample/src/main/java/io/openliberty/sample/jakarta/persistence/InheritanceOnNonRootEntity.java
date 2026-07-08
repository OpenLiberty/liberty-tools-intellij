package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

// Invalid: has @Entity + @Inheritance but extends another @Entity class — not the root
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class InheritanceOnNonRootEntity extends InheritanceEntityRoot {
    @Id
    private Long id;

    public InheritanceOnNonRootEntity() {
    }
}
