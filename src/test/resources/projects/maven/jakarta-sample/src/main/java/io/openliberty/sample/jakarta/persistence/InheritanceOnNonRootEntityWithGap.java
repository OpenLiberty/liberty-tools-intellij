package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

// Invalid: @Entity + @Inheritance but InheritanceGapEntityRoot is the actual root,
// hidden behind a non-entity abstract gap — full chain walk is required to detect this
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class InheritanceOnNonRootEntityWithGap extends InheritanceAbstractGap {
    @Id
    private Long id;

    public InheritanceOnNonRootEntityWithGap() {
    }
}
