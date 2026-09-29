package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Valid: @AttributeOverrides container — both "address" and "id" exist in Person.
 * Expected: no diagnostic.
 */
@Entity
@AttributeOverrides({
    @AttributeOverride(name = "address", column = @Column(name = "MGR_ADDR")),
    @AttributeOverride(name = "id",      column = @Column(name = "MGR_ID"))
})
public class ValidContainerOverride extends Person {
}
