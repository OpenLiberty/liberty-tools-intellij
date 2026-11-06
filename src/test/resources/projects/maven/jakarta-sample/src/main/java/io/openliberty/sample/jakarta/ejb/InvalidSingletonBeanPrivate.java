package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Singleton;

@Singleton
public class InvalidSingletonBeanPrivate {
    private String config;

    // Private constructor - should trigger diagnostic
    private InvalidSingletonBeanPrivate(String config) {
        this.config = config;
    }

    public String getConfig() {
        return config;
    }
}
