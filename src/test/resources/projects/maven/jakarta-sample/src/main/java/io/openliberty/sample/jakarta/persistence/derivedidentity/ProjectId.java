package io.openliberty.sample.jakarta.persistence.derivedidentity;

import java.io.Serializable;
import jakarta.persistence.Embeddable;

/**
 * @EmbeddedId class with a long empId and long projectId.
 */
@Embeddable
public class ProjectId implements Serializable {
    private static final long serialVersionUID = 1L;
    public long empId;
    public long projectId;
}
