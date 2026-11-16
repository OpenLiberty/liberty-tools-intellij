package io.openliberty.sample.jakarta.persistence.entitylisteners;

/**
 * A class nested directly inside an interface is implicitly static per JLS 9.5.
 * It should NOT be treated as a non-static inner class and must not produce a diagnostic.
 */
public interface InterfaceHolderWithListener {

    // Nested inside an interface -> implicitly static; valid as an entity listener.
    class ListenerInsideInterface {
        public ListenerInsideInterface() {
        }
    }
}
