package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * Valid entity with @IdClass referencing a key class that has no explicit constructor.
 * No diagnostics expected.
 */
@Entity
@IdClass(ImplicitConstructorIdClass.class)
public class ValidIdClassImplicitConstructor {

    @Id
    private String firstName;

    @Id
    private String lastName;
}
