package io.openliberty.sample.jakarta.jax_rs;

import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.PathParam;

public class RequestBean {
    @PathParam("userId")
    private String userId;

    @QueryParam("sort")
    private String sort;

}

