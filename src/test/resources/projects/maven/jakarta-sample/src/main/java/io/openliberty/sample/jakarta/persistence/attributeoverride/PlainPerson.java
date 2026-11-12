package io.openliberty.sample.jakarta.persistence.attributeoverride;

/**
 * Plain intermediate class (NOT annotated with @MappedSuperclass) that extends
 * the @MappedSuperclass Person. Used for chains with a non-mapped intermediate.
 */
public abstract class PlainPerson extends Person {
}
