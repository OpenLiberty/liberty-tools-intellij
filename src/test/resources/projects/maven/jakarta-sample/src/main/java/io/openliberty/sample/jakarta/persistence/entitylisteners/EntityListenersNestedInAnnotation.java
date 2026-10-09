package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Listener nested inside an annotation type is implicitly static (JLS 9.6) — valid, no diagnostic expected
@Entity
@EntityListeners(ImplicitlyStaticMemberHolder.ListenerAnnotation.NestedInAnnotation.class)
public class EntityListenersNestedInAnnotation {

    @Id
    private int id;
}
