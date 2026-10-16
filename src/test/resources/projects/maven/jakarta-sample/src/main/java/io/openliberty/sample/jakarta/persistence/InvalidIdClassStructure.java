package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * Entity with @IdClass referencing an invalid primary key class.
 * InvalidIdClass is: not public, has no public no-arg constructor, does not
 * implement Serializable, and does not declare equals or hashCode.
 * Expected: 5 diagnostics on the @IdClass annotation.
 */
@Entity
@IdClass(InvalidIdClass.class)
public class InvalidIdClassStructure {

    @Id
    private String firstName;

    @Id
    private String lastName;

    public InvalidIdClassStructure() {
    }
}
