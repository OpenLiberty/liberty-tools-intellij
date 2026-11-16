package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Listener nested inside a record is implicitly static (JLS 8.10) — valid, no diagnostic expected
@Entity
@EntityListeners(ImplicitlyStaticMemberHolder.ListenerRecord.NestedInRecord.class)
public class EntityListenersNestedInRecord {

    @Id
    private int id;
}
