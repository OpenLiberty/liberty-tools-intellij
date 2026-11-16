package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateless;

@Stateless
public class InvalidStatelessBeanPublic {
    private String name;

    // Public parameterized constructor - should trigger diagnostic
    public InvalidStatelessBeanPublic(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
