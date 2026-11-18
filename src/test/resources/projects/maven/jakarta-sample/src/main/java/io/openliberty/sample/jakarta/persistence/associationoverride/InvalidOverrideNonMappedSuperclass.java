package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * Invalid: class-level @AssociationOverride with name="supervisor" which exists in PlainBase,
 * but PlainBase is not annotated with @MappedSuperclass.
 * Expected: diagnostic InvalidAssociationOverrideName on the @AssociationOverride annotation.
 */
@Entity
@AssociationOverride(name = "supervisor", joinColumns = @JoinColumn(name = "SUPER_ID"))
public class InvalidOverrideNonMappedSuperclass extends PlainBase {
    @Id
    private Long id;
}
