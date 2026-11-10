package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

// Invalid: @MappedSuperclass is not @Entity — @Inheritance must not appear here
@MappedSuperclass
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class InheritanceOnMappedSuperclass {
    @Id
    private Long id;
}
