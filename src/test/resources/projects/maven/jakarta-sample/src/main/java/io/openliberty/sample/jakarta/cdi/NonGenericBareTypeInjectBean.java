package io.openliberty.sample.jakarta.cdi;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class NonGenericBareTypeInjectBean {

    // Invalid: method-level bare type variable U as @Inject method parameter
    @Inject
    public <U> void setMethodLevelBareType(U value) {
    }

    // Invalid: array of method-level bare type variable U[] as @Inject method parameter
    @Inject
    public <U> void setMethodLevelBareTypeArray(U[] values) {
    }

    // Valid: concrete type — no diagnostic
    @Inject
    public void setConcrete(String value) {
    }
}
