package io.openliberty.sample.jakarta.persistence;

/**
 * An invalid @IdClass key class for the MappedSuperclass test: not public,
 * has only a private constructor, does not implement Serializable,
 * and does not declare equals or hashCode.
 * Expected diagnostics (5) on the @IdClass annotation in InvalidMappedSuperclassIdClassStructure.
 */
class InvalidMappedSuperclassIdClass {

    private InvalidMappedSuperclassIdClass(String value) {
    }
}
