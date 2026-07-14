package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateless;

@Stateless
public class ValidStatelessBean {
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}