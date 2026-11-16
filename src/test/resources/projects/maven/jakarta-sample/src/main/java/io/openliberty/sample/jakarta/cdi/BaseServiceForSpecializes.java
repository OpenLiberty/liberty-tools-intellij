package io.openliberty.sample.jakarta.cdi;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A CDI bean with @ApplicationScoped scope annotation for SpecializesWithBeanSuperclass.
 */
@ApplicationScoped
public class BaseServiceForSpecializes {
    public String greet() { return "Hello"; }
}
