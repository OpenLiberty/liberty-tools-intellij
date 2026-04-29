package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Version;

@Entity
public class EntityDuplicateVersionMethods {
    
    private int version1;
    private int version2;
    
    public EntityDuplicateVersionMethods() {
    }
    
    @Version
    public int getVersion1() {
        return version1;
    }
    
    @Version
    public int getVersion2() {
        return version2;
    }
}
