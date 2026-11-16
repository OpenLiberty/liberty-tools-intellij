package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Two valid listeners: explicit public no-arg constructor and implicit default constructor
@Entity
@EntityListeners({ ExplicitPublicConstructorListener.class, ImplicitDefaultConstructorListener.class })
public class EntityListenersValidConstructor {

    @Id
    private int id;
}
