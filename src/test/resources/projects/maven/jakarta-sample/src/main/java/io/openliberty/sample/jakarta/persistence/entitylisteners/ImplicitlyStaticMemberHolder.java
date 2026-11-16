package io.openliberty.sample.jakarta.persistence.entitylisteners;

/**
 * Holder demonstrating implicitly-static member types per the JLS.
 *
 * <ul>
 *   <li>A class nested inside an interface is implicitly static (JLS 9.5).</li>
 *   <li>A class nested inside an enum is implicitly static (JLS 8.9).</li>
 *   <li>A class nested inside a record is implicitly static (JLS 8.10).</li>
 *   <li>A class nested inside an annotation type is implicitly static (JLS 9.6).</li>
 * </ul>
 *
 * None of these should be treated as non-static inner classes by the entity-listener
 * diagnostic — PSI synthesises the {@code static} modifier for all of them.
 */
public class ImplicitlyStaticMemberHolder {

    // --- nested inside an interface (JLS 9.5) ---
    public interface ListenerInterface {
        // Class nested inside an interface is implicitly static.
        class NestedInInterface {
            public NestedInInterface() {
            }
        }
    }

    // --- nested inside an enum (JLS 8.9) ---
    public enum ListenerEnum {
        INSTANCE;

        // Class nested inside an enum is implicitly static.
        public static class NestedInEnum {
            public NestedInEnum() {
            }
        }
    }

    // --- nested inside a record (JLS 8.10) ---
    public record ListenerRecord(int value) {
        // Class nested inside a record is implicitly static.
        public static class NestedInRecord {
            public NestedInRecord() {
            }
        }
    }

    // --- nested inside an annotation type (JLS 9.6) ---
    public @interface ListenerAnnotation {
        // Class nested inside an annotation type is implicitly static.
        class NestedInAnnotation {
            public NestedInAnnotation() {
            }
        }
    }
}
