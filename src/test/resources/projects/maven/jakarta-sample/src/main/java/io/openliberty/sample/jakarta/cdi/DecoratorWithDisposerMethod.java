package io.openliberty.sample.jakarta.cdi;

import jakarta.decorator.Decorator;
import jakarta.enterprise.inject.Disposes;

@Decorator
public class DecoratorWithDisposerMethod {

    // This method should trigger a diagnostic - decorator with @Disposes
    public void disposerMethod(@Disposes String resource) {

    }

    // This method should not trigger a diagnostic - no disposer annotation
    public void normalMethod(String param) {

    }
}
