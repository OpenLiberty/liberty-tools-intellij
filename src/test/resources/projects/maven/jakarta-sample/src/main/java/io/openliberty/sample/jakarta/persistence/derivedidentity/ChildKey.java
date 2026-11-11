package io.openliberty.sample.jakarta.persistence.derivedidentity;

import jakarta.persistence.Embeddable;

/**
 * Embeddable key whose parentKey attribute has type ProjectId (same as ParentWithEmbeddedId's PK);
 * wrongKey is a String and does not match.
 */
@Embeddable
public class ChildKey {
    public ProjectId parentKey;
    public String wrongKey;
    public int seq;
}
