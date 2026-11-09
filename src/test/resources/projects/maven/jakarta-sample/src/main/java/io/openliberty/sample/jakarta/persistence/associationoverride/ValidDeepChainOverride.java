package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

/**
 * Valid: name="teamLead" resolves to NamedPerson.teamLead (depth-2 MappedSuperclass chain).
 * Expected: no diagnostic.
 */
@Entity
@AssociationOverride(name = "teamLead", joinColumns = @JoinColumn(name = "EMP_TEAMLEAD_ID"))
public class ValidDeepChainOverride extends NamedPerson {
}
