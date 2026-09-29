package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;

/**
 * MappedSuperclass with @IdClass referencing an invalid primary key class.
 * InvalidMappedSuperclassIdClass is: not public, has no public no-arg constructor,
 * does not implement Serializable, and does not declare equals or hashCode.
 * Expected: 5 diagnostics on the @IdClass annotation.
 */
@MappedSuperclass
@IdClass(InvalidMappedSuperclassIdClass.class)
public class InvalidMappedSuperclassIdClassStructure {

    @Id
    private String firstName;

    @Id
    private String lastName;
}
