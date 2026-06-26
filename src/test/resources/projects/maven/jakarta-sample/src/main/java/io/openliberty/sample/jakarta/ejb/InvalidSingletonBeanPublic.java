package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Singleton;

@Singleton
public class InvalidSingletonBeanPublic {
    private String config;

    // Public parameterized constructor - should trigger diagnostic
    public InvalidSingletonBeanPublic(String config) {
        this.config = config;
    }

    public String getConfig() {
        return config;
    }
}
