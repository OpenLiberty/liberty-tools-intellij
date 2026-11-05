package com.example.wsrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GreetingResourceTest {

    private static HttpServer server;
    private static Client client;
    private static final URI BASE = URI.create("http://localhost:9998/");

    @BeforeAll
    static void start() {
        server = JdkHttpServerFactory.createHttpServer(BASE, new ResourceConfig(GreetingResource.class));
        client = ClientBuilder.newClient();
    }

    @AfterAll
    static void stop() {
        client.close();
        server.stop(0);
    }

    @Test
    void helloEndpoint() {
        try (Response r = client.target(BASE).path("greetings").request().get()) {
            assertEquals(200, r.getStatus());
            assertEquals("Hello, Jakarta REST 3.1!", r.readEntity(String.class));
        }
    }

    @Test
    void pathAndQueryParams() {
        try (Response r = client.target(BASE).path("greetings/Bob").queryParam("punctuation", "?").request().get()) {
            assertEquals("Hello, Bob?", r.readEntity(String.class));
        }
    }
}
