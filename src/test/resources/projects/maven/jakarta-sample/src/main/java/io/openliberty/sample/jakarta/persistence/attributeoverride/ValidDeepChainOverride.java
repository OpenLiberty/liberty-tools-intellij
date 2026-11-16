package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Valid: name="salary" resolves to NamedPerson.salary (depth-2 MappedSuperclass chain).
 * Expected: no diagnostic.
 */
@Entity
@AttributeOverride(name = "salary", column = @Column(name = "EMP_SALARY"))
public class ValidDeepChainOverride extends NamedPerson {
}
