package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Invalid: @AttributeOverrides container on a plain Long @Id field that is not
 * annotated with @Embedded, @EmbeddedId, or @ElementCollection.
 * Expected: diagnostic AttributeOverrideOnNonEmbeddedField on the @AttributeOverrides annotation.
 */
@Entity
public class InvalidAttributeOverrideOnIdField {
    @Id
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "EMP_NAME"))
    })
    private Long id;
}
