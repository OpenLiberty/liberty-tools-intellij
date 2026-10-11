package io.openliberty.sample.jakarta.persistence.derivedidentity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

/**
 * VALID (spec Example 3b): the parent has @EmbeddedId (ProjectId) and ChildKey.parentKey is ProjectId.
 * Expects: no diagnostics
 */
@Entity
public class ValidMapsIdCompositeParent {

    @EmbeddedId
    private ChildKey id;

    @MapsId("parentKey")
    @ManyToOne
    private ParentWithEmbeddedId parent;
}
