package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateless;

@Stateless
public class InvalidStatelessBeanPrivate {
    private String name;

    // Private constructor - should trigger diagnostic
    private InvalidStatelessBeanPrivate(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
