package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateful;

@Stateful
public class InvalidStatefulBeanFinalize {
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    // Session beans must not define finalize() method
    @Override
    protected void finalize() throws Throwable {
        // Cleanup code - this violates EJB specification
        count = 0;
    }
}