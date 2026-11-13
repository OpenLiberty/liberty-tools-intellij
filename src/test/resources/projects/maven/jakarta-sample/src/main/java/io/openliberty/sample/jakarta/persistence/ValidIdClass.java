package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

/**
 * A valid @IdClass key class: public, has a public no-arg constructor,
 * implements Serializable, and declares both equals and hashCode.
 * No diagnostics expected when this class is referenced by @IdClass.
 */
@Embeddable
public class ValidIdClass implements Serializable {

    private static final long serialVersionUID = 1L;

    public String firstName;
    public String lastName;

    public ValidIdClass() {
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ValidIdClass;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
