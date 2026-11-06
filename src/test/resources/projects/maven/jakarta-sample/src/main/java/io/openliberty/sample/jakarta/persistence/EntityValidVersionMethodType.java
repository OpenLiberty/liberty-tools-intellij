package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class EntityValidVersionMethodType {

    @Id
    private Long id;

    private long version; // Valid: long is a supported type

    public EntityValidVersionMethodType() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Version
    public long getVersion() { // Valid: long return type
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}

