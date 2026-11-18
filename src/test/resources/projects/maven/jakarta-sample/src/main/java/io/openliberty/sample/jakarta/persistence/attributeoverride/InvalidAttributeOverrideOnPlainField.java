package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Invalid: @AttributeOverride on a plain String field that is not annotated
 * with @Embedded, @EmbeddedId, or @ElementCollection.
 * Expected: diagnostic AttributeOverrideOnNonEmbeddedField on the @AttributeOverride annotation.
 */
@Entity
public class InvalidAttributeOverrideOnPlainField {
    @Id
    private Long id;

    @AttributeOverride(name = "city", column = @Column(name = "EMP_CITY"))
    private String city;
}
