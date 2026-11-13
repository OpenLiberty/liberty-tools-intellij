package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Invalid: class-level @AttributeOverride on an entity that has no superclass,
 * so "city" cannot be an attribute inherited from a @MappedSuperclass.
 * Expected: diagnostic InvalidAttributeOverrideName on the @AttributeOverride annotation.
 */
@Entity
@AttributeOverride(name = "city", column = @Column(name = "EMP_CITY"))
public class InvalidOverrideNoSuperclass {
    @Id
    private Long id;
    private String city;
}
