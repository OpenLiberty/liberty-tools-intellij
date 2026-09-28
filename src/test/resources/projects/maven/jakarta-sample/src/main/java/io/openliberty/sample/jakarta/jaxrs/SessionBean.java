package io.openliberty.sample.jakarta.jax_rs;

import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.PathParam;

public class SessionBean {
    @PathParam("sessionId")
    private String sessionId;

    @QueryParam("invalidateSession")
    private boolean invalidateSession;

}
