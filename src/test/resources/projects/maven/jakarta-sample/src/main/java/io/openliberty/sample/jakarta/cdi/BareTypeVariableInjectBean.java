package io.openliberty.sample.jakarta.cdi;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class BareTypeVariableInjectBean<T> {

    // Invalid: bare type variable T as @Inject field type
    @Inject
    T bareTypeField;

    // Invalid: array of bare type variable T[] as @Inject field type
    @Inject
    T[] bareTypeArrayField;

    // Invalid: bare type variable T as @Inject method parameter
    @Inject
    public void setBareType(T value) {
    }

    // Invalid: array of bare type variable T[] as @Inject method parameter
    @Inject
    public void setBareTypeArray(T[] values) {
    }

    // Invalid: multi-parameter method with bare type variables
    @Inject
    public void setMultipleParams(String validParam, T bareParam, int count, T[] bareArrayParam) {
    }

    // Valid: parameterized type with type variable (List<T>)
    @Inject
    List<T> genericList;

    // Valid: concrete type — no diagnostic
    @Inject
    String concreteField;
}
