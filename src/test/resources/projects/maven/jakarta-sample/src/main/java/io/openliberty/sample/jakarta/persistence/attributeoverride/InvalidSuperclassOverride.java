package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Invalid: name="salary" does not exist in Person (only "id" and "address").
 * Expected: diagnostic InvalidAttributeOverrideName on the @AttributeOverride annotation.
 */
@Entity
@AttributeOverride(name = "salary", column = @Column(name = "EMP_SALARY"))
public class InvalidSuperclassOverride extends Person {
}
