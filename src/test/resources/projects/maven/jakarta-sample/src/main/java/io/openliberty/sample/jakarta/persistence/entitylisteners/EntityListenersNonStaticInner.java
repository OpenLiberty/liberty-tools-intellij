package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Both non-static inner classes are non-instantiable as entity listeners
@Entity
@EntityListeners({ OuterListenerContainer.NonStaticInnerImplicitListener.class, OuterListenerContainer.NonStaticInnerExplicitListener.class })
public class EntityListenersNonStaticInner {

    @Id
    private int id;
}
