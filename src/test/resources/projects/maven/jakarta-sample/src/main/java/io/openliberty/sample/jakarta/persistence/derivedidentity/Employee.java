package io.openliberty.sample.jakarta.persistence.derivedidentity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Parent entity with a simple long @Id.
 */
@Entity
public class Employee {
    @Id
    private long id;
    private String name;
}
