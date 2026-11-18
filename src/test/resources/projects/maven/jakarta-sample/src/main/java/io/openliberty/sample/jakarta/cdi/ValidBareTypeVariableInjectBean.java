package io.openliberty.sample.jakarta.cdi;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class ValidBareTypeVariableInjectBean {

    @Inject
    private String stringField;

    @Inject
    private List<String> listField;

    @Inject
    public void setString(String value) {
    }

    @Inject
    public void setMultiple(String first, int second, List<String> third) {
    }
}
