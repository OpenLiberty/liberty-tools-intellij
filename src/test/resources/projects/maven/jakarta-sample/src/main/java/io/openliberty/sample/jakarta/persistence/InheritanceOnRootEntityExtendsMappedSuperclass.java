package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

// Valid: @Entity + @Inheritance extending a @MappedSuperclass — @MappedSuperclass is not
// @Entity so the full chain contains no @Entity ancestor
@MappedSuperclass
abstract class InheritanceMappedSuperclassBase {
    private String createdBy;
}

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class InheritanceOnRootEntityExtendsMappedSuperclass extends InheritanceMappedSuperclassBase {
    @Id
    private Long id;

    public InheritanceOnRootEntityExtendsMappedSuperclass() {
    }
}
