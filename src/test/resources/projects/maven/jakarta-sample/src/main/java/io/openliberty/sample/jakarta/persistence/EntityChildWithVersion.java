package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Version;

@Entity
public class EntityChildWithVersion extends EntityParentWithVersion {
    
    @Version
    private int childVersion;
    
    public EntityChildWithVersion() {
        super();
    }
}

