package io.openliberty.sample.jakarta.persistence.associationoverride;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Simple entity used as a relationship target in association override tests.
 */
@Entity
public class Employee {
    @Id
    private Long id;
    private String name;
}
