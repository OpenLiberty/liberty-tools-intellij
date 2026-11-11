package io.openliberty.sample.jakarta.persistence.derivedidentity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Parent entity with a simple String @Id.
 */
@Entity
public class Department {
    @Id
    private String id;
    private String name;
}
