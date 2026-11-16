package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Invalid: container with "id" (valid) and "bonus" (invalid — not in Person).
 * Expected: diagnostic on "bonus" entry only.
 */
@Entity
@AttributeOverrides({
    @AttributeOverride(name = "id",    column = @Column(name = "MGR_ID")),
    @AttributeOverride(name = "bonus", column = @Column(name = "MGR_BONUS"))
})
public class InvalidContainerOneEntry extends Person {
}
