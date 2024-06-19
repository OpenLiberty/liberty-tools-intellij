/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.it;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EndpointIT {
    private static String baseUrl;
    private static final String RESOURCE_ENDPOINT = "/api/resource";
    private static final String LIVENESS_ENDPOINT = "/health/live";
    private static final String READINESS_ENDPOINT = "/health/ready";
    private final String expectedGreeting = "Hello! Welcome to Open Liberty";
    private Response response;
    private static Client client;

    @BeforeAll
    public static void init() {
        String port = System.getProperty("http.port");
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    public static void destroy() throws Exception {
        client.close();
    }

    @BeforeEach
    public void setup() {
        response = null;
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void teardown() {
        response.close();
        client.close();
    }

    @Test
    public void testApplicationResponse() {
        String livenessURL = baseUrl + RESOURCE_ENDPOINT;
        response = this.getResponse(baseUrl + RESOURCE_ENDPOINT);
        this.assertResponse(livenessURL, response);
        String msg = response.readEntity(String.class);
        assertEquals(expectedGreeting, msg);
    }

    @Test
    public void testLivenessEndpoint() {
        String livenessURL = baseUrl + LIVENESS_ENDPOINT;
        response = this.getResponse(baseUrl + LIVENESS_ENDPOINT);
        this.assertResponse(livenessURL, response);

        JsonObject healthJson = response.readEntity(JsonObject.class);
        String expectedOutcome = "UP";
        String actualOutcome = healthJson.getString("status");
        assertEquals(expectedOutcome, actualOutcome,
                "Applications liveness check passed");
    }

    @Test
    public void testReadinessEndpoint() {
        String readinessURL = baseUrl + READINESS_ENDPOINT;
        response = this.getResponse(baseUrl + READINESS_ENDPOINT);
        this.assertResponse(readinessURL, response);

        JsonObject healthJson = response.readEntity(JsonObject.class);
        String expectedOutcome = "UP";
        String actualOutcome = healthJson.getString("status");
        assertEquals(expectedOutcome, actualOutcome,
                "Applications readiness check passed");
    }

    /**
     * <p>
     * Returns response information from the specified URL.
     * </p>
     *
     * @param url - target URL.
     * @return Response object with the response from the specified URL.
     */
    private Response getResponse(String url) {
        return client.target(url).request().get();
    }

    /**
     * <p>
     * Asserts that the given URL has the correct response code of 200.
     * </p>
     *
     * @param url      - target URL.
     * @param response - response received from the target URL.
     */
    private void assertResponse(String url, Response response) {
        assertEquals(200, response.getStatus(), "Incorrect response code from " + url);
    }
}
