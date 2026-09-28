package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class EntityDuplicateVersion {

    @Id
    private int id;

    @Version
    private int version1;
    
    @Version
    private int version2;
    
    public EntityDuplicateVersion() {
    }
}
