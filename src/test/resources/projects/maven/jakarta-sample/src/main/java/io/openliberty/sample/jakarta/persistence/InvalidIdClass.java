package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Embeddable;

/**
 * An invalid @IdClass key class: package-private, has only a private constructor,
 * does not implement Serializable, and does not declare equals or hashCode.
 * Expected diagnostics (5): not public, no public no-arg constructor,
 * not Serializable, no equals, no hashCode.
 */
@Embeddable
class InvalidIdClass {

    public String firstName;
    public String lastName;

    private InvalidIdClass(String value) {
    }
}
