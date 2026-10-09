package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Listener nested inside an enum is implicitly static (JLS 8.9) — valid, no diagnostic expected
@Entity
@EntityListeners(ImplicitlyStaticMemberHolder.ListenerEnum.NestedInEnum.class)
public class EntityListenersNestedInEnum {

    @Id
    private int id;
}
