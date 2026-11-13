package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * Valid entity with @IdClass referencing a properly structured primary key class.
 * No diagnostics expected.
 */
@Entity
@IdClass(ValidIdClass.class)
public class ValidIdClassStructure {

    @Id
    private String firstName;

    @Id
    private String lastName;
}
