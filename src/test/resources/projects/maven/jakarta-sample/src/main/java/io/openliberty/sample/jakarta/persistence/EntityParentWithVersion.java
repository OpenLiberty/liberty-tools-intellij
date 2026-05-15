package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public class EntityParentWithVersion {

    @Id
    private int id;

    @Version
    private int version;
    
    public EntityParentWithVersion() {
    }
}
