package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Listener nested inside an interface is implicitly static (JLS 9.5) — valid, no diagnostic expected
@Entity
@EntityListeners(InterfaceHolderWithListener.ListenerInsideInterface.class)
public class EntityListenersInterfaceNestedListener {

    @Id
    private int id;
}
