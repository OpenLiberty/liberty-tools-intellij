package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

// Invalid: @Inheritance is present but @Entity is missing
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class InheritanceOnNonEntity {
    private Long id;
}
