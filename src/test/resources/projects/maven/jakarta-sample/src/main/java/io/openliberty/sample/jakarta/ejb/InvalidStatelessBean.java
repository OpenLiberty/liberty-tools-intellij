package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateless;

@Stateless
public class InvalidStatelessBean {
    private String name;

    // Private constructor - should trigger diagnostic
    private InvalidStatelessBean(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}