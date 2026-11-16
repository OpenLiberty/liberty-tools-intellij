package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateless;

@Stateless
public class InvalidStatelessBeanFinalize {
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    // Session beans must not define finalize() method
    @Override
    protected void finalize() throws Throwable {
        // Cleanup code - this violates EJB specification
        data = null;
    }
}