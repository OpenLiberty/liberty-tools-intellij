package io.openliberty.sample.jakarta.persistence;

import java.io.Serializable;

/**
 * An @IdClass key class that is public, has a public no-arg constructor, inherits
 * Serializable from its superclass and declares hashCode, but its equals method
 * takes PartialInvalidIdClass instead of Object, so it does not override Object.equals.
 * Expected diagnostics (1): no equals(Object).
 */
public class PartialInvalidIdClass extends SerializableBaseKey {

    public PartialInvalidIdClass() {
    }

    public boolean equals(PartialInvalidIdClass other) {
        return other != null;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

class SerializableBaseKey implements Serializable {
    private static final long serialVersionUID = 1L;
}
