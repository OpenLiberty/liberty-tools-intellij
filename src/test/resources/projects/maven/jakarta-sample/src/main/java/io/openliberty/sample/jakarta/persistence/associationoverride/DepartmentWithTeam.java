package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToOne;

/**
 * Embeddable with a nested embeddable field (subDept) for dot-notation tests.
 * Fields: manager, lead (associations) and subDept (nested embeddable of type SubTeam).
 */
@Embeddable
public class DepartmentWithTeam {
    @ManyToOne
    private Employee manager;

    @ManyToOne
    private Employee lead;

    @Embedded
    private SubTeam subDept;
}
