package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Singleton;

@Singleton
public class InvalidSingletonBean {
    private String config;

    // Private constructor - should trigger diagnostic
    private InvalidSingletonBean(String config) {
        this.config = config;
    }

    public String getConfig() {
        return config;
    }
}