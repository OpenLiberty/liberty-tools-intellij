package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Two invalid listeners: protected no-arg constructor and parameterized-only constructor
@Entity
@EntityListeners({ ProtectedConstructorListener.class, ParameterizedConstructorListener.class })
public class EntityListenersInvalidConstructor {

    @Id
    private int id;
}
