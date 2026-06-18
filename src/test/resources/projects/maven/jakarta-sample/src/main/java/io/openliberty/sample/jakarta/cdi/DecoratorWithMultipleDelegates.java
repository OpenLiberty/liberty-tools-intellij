package io.openliberty.sample.jakarta.cdi;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

interface GreetingService {
    String greet(String name);
}

@Decorator
public class DecoratorWithMultipleDelegates implements GreetingService {

    // First delegate: field injection
    @Inject
    @Delegate
    private GreetingService fieldDelegate;

    // Second delegate: constructor parameter injection
    @Inject
    public DecoratorWithMultipleDelegates(@Delegate GreetingService constructorDelegate) {
    }

    // Third delegate: method parameter injection
    @Inject
    public void setMethodDelegate(@Delegate GreetingService methodDelegate) {
    }

    @Override
    public String greet(String name) {
        return fieldDelegate.greet(name);
    }
}
