package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateful;

@Stateful
public class InvalidStatefulBeanPublic {
    private int count;

    // Public parameterized constructor - should trigger diagnostic
    public InvalidStatefulBeanPublic(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
