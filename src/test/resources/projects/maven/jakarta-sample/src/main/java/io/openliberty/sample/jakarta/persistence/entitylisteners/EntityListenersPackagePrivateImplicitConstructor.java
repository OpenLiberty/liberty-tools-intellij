package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// Uses a package-private class with no declared constructor — implicit constructor is package-private, not public
@Entity
@EntityListeners(PackagePrivateImplicitConstructorListener.class)
public class EntityListenersPackagePrivateImplicitConstructor {

    @Id
    private int id;
}
