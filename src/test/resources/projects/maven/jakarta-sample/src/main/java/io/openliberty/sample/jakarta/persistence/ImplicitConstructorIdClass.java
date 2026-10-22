package io.openliberty.sample.jakarta.persistence;

import java.io.Serializable;

/**
 * A valid @IdClass key class with no explicit constructor. The implicit default
 * constructor of a public class is public, so no diagnostics are expected.
 */
public class ImplicitConstructorIdClass implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object other) {
        return other instanceof ImplicitConstructorIdClass;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
