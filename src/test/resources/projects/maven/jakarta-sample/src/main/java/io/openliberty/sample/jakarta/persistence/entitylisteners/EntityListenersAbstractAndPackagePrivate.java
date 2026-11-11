package io.openliberty.sample.jakarta.persistence.entitylisteners;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

// AbstractListener is non-instantiable; PackagePrivateConstructorListener has invalid constructor
@Entity
@EntityListeners({ AbstractListener.class, PackagePrivateConstructorListener.class })
public class EntityListenersAbstractAndPackagePrivate {

    @Id
    private int id;
}
