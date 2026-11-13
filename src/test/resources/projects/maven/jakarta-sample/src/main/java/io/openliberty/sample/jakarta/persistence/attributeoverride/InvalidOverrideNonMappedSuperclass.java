package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Invalid: class-level @AttributeOverride with name="city" which exists in PlainBase,
 * but PlainBase is not annotated with @MappedSuperclass.
 * Expected: diagnostic InvalidAttributeOverrideName on the @AttributeOverride annotation.
 */
@Entity
@AttributeOverride(name = "city", column = @Column(name = "EMP_CITY"))
public class InvalidOverrideNonMappedSuperclass extends PlainBase {
    @Id
    private Long id;
}
