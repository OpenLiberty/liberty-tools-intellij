package io.openliberty.sample.jakarta.cdi;

import jakarta.decorator.Decorator;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

@Decorator
public class DecoratorWithObserverMethod {

    public DecoratorWithObserverMethod() {
    }

    // Invalid: Decorator with @Observes parameter
    public void observerMethod(@Observes String event) {
        System.out.println("Observer method: " + event);
    }

    // Invalid: Decorator with @ObservesAsync parameter
    public void observerAsyncMethod(@ObservesAsync String event) {
        System.out.println("Observer async method: " + event);
    }

    // Valid: Regular method without observer annotations
    public void regularMethod(String param) {
        System.out.println("Regular method: " + param);
    }
}

// Made with Bob
