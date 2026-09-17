package io.openliberty.sample.jakarta.persistence.entitylisteners;

/**
 * Container class providing non-static inner class and static nested class listener
 * variants used to test @EntityListeners diagnostics.
 */
public class OuterListenerContainer {

    // Non-static inner class with implicit constructor - non-instantiable as entity listener
    public class NonStaticInnerImplicitListener {
    }

    // Non-static inner class with explicit constructor - non-instantiable as entity listener
    public class NonStaticInnerExplicitListener {
        public NonStaticInnerExplicitListener() {
        }
    }

    // Static nested class with implicit constructor - valid
    public static class StaticNestedImplicitListener {
    }

    // Static nested class with explicit public no-arg constructor - valid
    public static class StaticNestedExplicitListener {
        public StaticNestedExplicitListener() {
        }
    }
}
