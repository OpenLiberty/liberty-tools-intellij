package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * MappedSuperclass with fields: id, address.
 * Used as the superclass target for @AttributeOverride validation tests.
 */
@MappedSuperclass
public abstract class Person {
    @Id
    protected Long id;
    protected String address;
}
