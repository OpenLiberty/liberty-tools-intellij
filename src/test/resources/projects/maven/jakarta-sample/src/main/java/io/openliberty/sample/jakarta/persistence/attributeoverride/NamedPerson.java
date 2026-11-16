package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.MappedSuperclass;

/**
 * MappedSuperclass that extends Person.
 * Adds fields: name, salary — used for depth-2 chain validation tests.
 */
@MappedSuperclass
public abstract class NamedPerson extends Person {
    protected String name;
    protected int salary;
}
