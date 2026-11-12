package io.openliberty.sample.jakarta.persistence.attributeoverride;

/**
 * Plain superclass (NOT annotated with @MappedSuperclass) with field: city.
 * Used to verify that class-level @AttributeOverride only resolves against
 * attributes inherited from a @MappedSuperclass.
 */
public abstract class PlainBase {
    protected String city;
}
