package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import java.util.Map;

/**
 * Valid: @AttributeOverride with a "key." prefix on Map-valued @OneToMany / @ManyToMany
 * members whose key is an embeddable (Address). Spec 11.1.4 allows overrides on
 * "a map collection whose key and/or value is an embeddable class".
 * Expected: no diagnostic.
 */
@Entity
public class ValidMapRelationshipOverride {
    @Id
    private Long id;

    @OneToMany
    @AttributeOverride(name = "key.city", column = @Column(name = "KEY_CITY"))
    private Map<Address, NamedPerson> oneToManyByAddress;

    @ManyToMany
    @AttributeOverride(name = "key.street", column = @Column(name = "KEY_STREET"))
    private Map<Address, NamedPerson> manyToManyByAddress;
}
