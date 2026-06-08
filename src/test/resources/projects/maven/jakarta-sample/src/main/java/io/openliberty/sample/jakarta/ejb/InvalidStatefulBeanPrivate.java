package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateful;

@Stateful
public class InvalidStatefulBeanPrivate {
    private int count;

    // Private constructor - should trigger diagnostic
    private InvalidStatefulBeanPrivate(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
