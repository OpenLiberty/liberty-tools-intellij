package io.openliberty.sample.jakarta.cdi.sessionbean;

import jakarta.ejb.Singleton;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.ApplicationScoped;

// Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error
@Singleton
@RequestScoped
@ApplicationScoped
public class SingletonWithMixedInvalidAndApplicationScoped {
}
