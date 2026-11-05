package com.example.wsrs;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/greetings")
@Produces(MediaType.TEXT_PLAIN)
public class GreetingResource {

    @GET
    public String hello() {
        return "Hello, Jakarta REST 4.0!";
    }

    @GET
    @Path("/{name}")
    public String greet(@PathParam("name") String name,
                        @QueryParam("punctuation") @DefaultValue("!") String punctuation) {
        return "Hello, " + name + punctuation;
    }
}
