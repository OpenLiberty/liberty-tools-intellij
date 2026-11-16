package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class EntityInvalidVersionMethodType {

    @Id
    private Long id;

    private String version; // Invalid: String is not a supported type

    public EntityInvalidVersionMethodType() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Version
    public String getVersion() { // Invalid: String return type
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

