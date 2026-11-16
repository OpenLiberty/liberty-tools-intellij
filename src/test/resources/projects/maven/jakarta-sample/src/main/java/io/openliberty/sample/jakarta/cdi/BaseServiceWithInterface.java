package io.openliberty.sample.jakarta.cdi;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A CDI bean with @ApplicationScoped scope annotation for SpecializesWithBeanSuperclassAndInterface.
 */
@ApplicationScoped
public class BaseServiceWithInterface {
    public String greet() { return "Hello"; }
}
