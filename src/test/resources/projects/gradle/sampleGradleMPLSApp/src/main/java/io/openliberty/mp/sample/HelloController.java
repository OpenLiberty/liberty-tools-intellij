package io.openliberty.mp.sample;

import io.openliberty.mp.sample.health.ServiceLiveHealthCheck;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 *
 */
@Path("/hello")
@Singleton
public class HelloController {

    @GET
    public String sayHello() {
        ServiceLiveHealthCheck slhc = new ServiceLiveHealthCheck();

        if (slhc.call().equals("up")) {
            return "Hello World";
        }
        return ("blah");
    }
}
