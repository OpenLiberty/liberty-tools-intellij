package io.openliberty.sample.jakarta.persistence.idclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(OrderIdWithEmbeddable.class)
public class OrderWithValidIdClass {

    @Id
    private Long customerId;

    @Id
    private Long orderNumber;
}
