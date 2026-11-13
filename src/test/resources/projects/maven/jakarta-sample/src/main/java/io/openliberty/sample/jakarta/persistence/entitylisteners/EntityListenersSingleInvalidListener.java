package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Single-class form @EntityListeners(L.class) with a listener that has only a protected constructor — invalid
@Entity
@EntityListeners(ProtectedConstructorListener.class)
public class EntityListenersSingleInvalidListener {

    @Id
    private int id;
}
