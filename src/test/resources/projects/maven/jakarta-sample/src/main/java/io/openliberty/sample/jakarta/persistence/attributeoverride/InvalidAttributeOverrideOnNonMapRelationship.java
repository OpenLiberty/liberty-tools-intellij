package io.openliberty.sample.jakarta.persistence.attributeoverride;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;

/**
 * Invalid: @AttributeOverride on a @OneToMany member that is a List, not a Map.
 * Expected: diagnostic AttributeOverrideOnNonEmbeddedField on the @AttributeOverride annotation.
 */
@Entity
public class InvalidAttributeOverrideOnNonMapRelationship {
    @Id
    private Long id;

    @OneToMany
    @AttributeOverride(name = "city", column = @Column(name = "EMP_CITY"))
    private List<NamedPerson> people;
}
