package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateful;

@Stateful
public class InvalidStatefulBean {
    private int count;

    // Private constructor - should trigger diagnostic
    private InvalidStatefulBean(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}