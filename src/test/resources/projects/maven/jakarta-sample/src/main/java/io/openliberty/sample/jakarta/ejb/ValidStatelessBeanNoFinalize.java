package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateless;

@Stateless
public class ValidStatelessBeanNoFinalize {
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    // This is valid - no finalize() method defined
    public void cleanup() {
        // Custom cleanup method is fine
        data = null;
    }
}