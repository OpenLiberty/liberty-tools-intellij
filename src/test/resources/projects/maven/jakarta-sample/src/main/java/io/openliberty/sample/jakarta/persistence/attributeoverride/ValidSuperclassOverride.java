package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Valid: class-level @AttributeOverride with name="address" which exists in Person.
 * Expected: no diagnostic.
 */
@Entity
@AttributeOverride(name = "address", column = @Column(name = "PERSON_ADDR"))
public class ValidSuperclassOverride extends Person {
}
