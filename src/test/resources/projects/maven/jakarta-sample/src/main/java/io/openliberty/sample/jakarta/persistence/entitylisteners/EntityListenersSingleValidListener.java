package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Single-class form @EntityListeners(L.class) with a valid listener — no diagnostic expected
@Entity
@EntityListeners(ExplicitPublicConstructorListener.class)
public class EntityListenersSingleValidListener {

    @Id
    private int id;
}
