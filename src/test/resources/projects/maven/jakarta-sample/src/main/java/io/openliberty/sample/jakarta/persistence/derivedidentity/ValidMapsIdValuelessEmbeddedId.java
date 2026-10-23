package io.openliberty.sample.jakarta.persistence.derivedidentity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

/**
 * VALID (spec Examples 5b/6b): embedded id type ProjectId matches the parent's PK type (ProjectId).
 * Expects: no diagnostics
 */
@Entity
public class ValidMapsIdValuelessEmbeddedId {

    @EmbeddedId
    private ProjectId id;

    @MapsId
    @OneToOne
    private ParentWithEmbeddedId parent;
}
