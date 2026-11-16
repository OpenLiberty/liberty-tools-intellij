package io.openliberty.sample.jakarta.persistence.derivedidentity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

/**
 * INVALID: valueless @MapsId with @EmbeddedId ChildKey, but parent's PK is ProjectId — type mismatch.
 * Expects: MapsIdPrimaryKeyTypeMismatch on field "parent"
 */
@Entity
public class MapsIdValuelessPkTypeMismatch {

    @EmbeddedId
    private ChildKey id;

    @MapsId
    @OneToOne
    private ParentWithEmbeddedId parent;
}
