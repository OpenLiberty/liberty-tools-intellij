package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Both static nested classes are valid entity listeners
@Entity
@EntityListeners({ OuterListenerContainer.StaticNestedImplicitListener.class, OuterListenerContainer.StaticNestedExplicitListener.class })
public class EntityListenersStaticNested {

    @Id
    private int id;
}
