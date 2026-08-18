package io.openliberty.mp.sample.system;

import jakarta.ws.rs.core.Response;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path("/properties")
public class SystemResource2 {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProperties() {

        return Response.ok(System.getProperties()).build();
    }

}