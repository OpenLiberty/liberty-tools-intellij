package io.openliberty.sample.jakarta.persistence.idclass;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

// Correctly annotated with @Embeddable — used to verify the valid case
@Embeddable
public class OrderIdWithEmbeddable implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long customerId;
    private Long orderNumber;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OrderIdWithEmbeddable that = (OrderIdWithEmbeddable) o;
        return Objects.equals(customerId, that.customerId) && Objects.equals(orderNumber, that.orderNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, orderNumber);
    }
}
