package io.openliberty.sample.jakarta.persistence.derivedidentity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

/**
 * VALID (spec Example 1b): @MapsId("empId") where ProjectId.empId (long) matches Employee's @Id (long).
 * Expects: no diagnostics
 */
@Entity
public class ValidDerivedIdentityEmbeddedId {

    @EmbeddedId
    private ProjectId id;

    @MapsId("empId")
    @ManyToOne
    private Employee employee;
}
