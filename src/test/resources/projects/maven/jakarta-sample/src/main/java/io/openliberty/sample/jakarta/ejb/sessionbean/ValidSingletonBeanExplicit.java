package io.openliberty.sample.jakarta.ejb.sessionbean;

import jakarta.ejb.Singleton;

// Valid: public Singleton with explicit constructor
@Singleton
public class ValidSingletonBeanExplicit {
    public ValidSingletonBeanExplicit() {
    }
}
