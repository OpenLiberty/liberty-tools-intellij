package io.openliberty.sample.jakarta.cdi.sessionbean;

import jakarta.ejb.Singleton;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.Dependent;

// Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - should report error
@Singleton
@SessionScoped
@Dependent
public class SingletonWithMixedInvalidAndDependent {
}
