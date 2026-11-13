package io.openliberty.sample.jakarta.persistence.idclass;

import java.io.Serializable;
import java.util.Objects;

// This composite key class intentionally lacks @Embeddable to trigger the diagnostic
public class OrderIdMissingEmbeddable implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long customerId;
    private Long orderNumber;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OrderIdMissingEmbeddable that = (OrderIdMissingEmbeddable) o;
        return Objects.equals(customerId, that.customerId) && Objects.equals(orderNumber, that.orderNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, orderNumber);
    }
}
