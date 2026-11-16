package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Singleton;

@Singleton
public class InvalidSingletonBeanFinalize {
    private String config;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    // Session beans must not define finalize() method
    @Override
    protected void finalize() throws Throwable {
        // Cleanup code - this violates EJB specification
        config = null;
    }
}