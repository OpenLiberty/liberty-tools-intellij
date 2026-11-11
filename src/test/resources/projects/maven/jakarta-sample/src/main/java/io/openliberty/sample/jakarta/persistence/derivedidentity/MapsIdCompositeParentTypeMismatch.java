package io.openliberty.sample.jakarta.persistence.derivedidentity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

/**
 * INVALID: the parent's PK is composite type ProjectId, but the @MapsId attribute
 * wrongKey in ChildKey is a String — type mismatch.
 * Expects: MapsIdValueTypeMismatch on field "parent"
 */
@Entity
public class MapsIdCompositeParentTypeMismatch {

    @EmbeddedId
    private ChildKey id;

    @MapsId("wrongKey")
    @ManyToOne
    private ParentWithEmbeddedId parent;
}
