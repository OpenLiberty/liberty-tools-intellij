package io.openliberty.sample.jakarta.ejb.sessionbean;

import jakarta.ejb.Stateless;

// Valid: public, non-final, non-abstract, top-level — explicit no-arg constructor
@Stateless
public class ValidStatelessBeanExplicit {
    public ValidStatelessBeanExplicit() {
    }
}
