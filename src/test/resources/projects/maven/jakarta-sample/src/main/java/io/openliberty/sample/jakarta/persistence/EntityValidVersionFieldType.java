package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.sql.Timestamp;

@Entity
public class EntityValidVersionFieldType {

    @Id
    private Long id;

    @Version
    private int version; // Valid: int is a supported type

    public EntityValidVersionFieldType() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
