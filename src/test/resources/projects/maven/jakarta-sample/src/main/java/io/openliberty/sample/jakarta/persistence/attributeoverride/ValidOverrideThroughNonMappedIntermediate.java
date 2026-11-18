package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Valid: name="address" resolves to Person.address (@MappedSuperclass) through
 * the non-mapped intermediate class PlainPerson.
 */
@Entity
@AttributeOverride(name = "address", column = @Column(name = "EMP_ADDR"))
public class ValidOverrideThroughNonMappedIntermediate extends PlainPerson {
}
