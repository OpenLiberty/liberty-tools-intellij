package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/** Entity whose @IdClass key class violates only the equals(Object) requirement. */
@Entity
@IdClass(PartialInvalidIdClass.class)
public class PartialInvalidIdClassStructure {

    @Id
    private String firstName;

    @Id
    private String lastName;
}
