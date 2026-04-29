package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Version;

@Entity
public class EntityParentWithVersion {
    
    @Version
    private int version;
    
    public EntityParentWithVersion() {
    }
}
