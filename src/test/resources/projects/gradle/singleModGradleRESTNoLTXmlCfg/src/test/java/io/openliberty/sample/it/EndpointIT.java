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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests.
 */
public class EndpointIT {

    /**
     * The base URL for this application.
     */
    private static String baseUrl;

    /**
     * The test resource endpoint.
     */
    private static final String RESOURCE_ENDPOINT = "/api/resource";

    /**
     * The message expected to come back when calling the test resoruce endpoint.
     */
    private String expectedGreeting = "Hello! Welcome to Open Liberty";

    /**
     * The request responce.
     */
    private Response response;

    /**
     * The REST client to issue request to the server.
     */
    private static Client client;

    /**
     * Setup prior to all tests running.
     */
    @BeforeAll
    public static void init() {
        String port = System.getProperty("http.port");
        baseUrl = "http://localhost:" + port;
    }

    /**
     * Cleanup after all tests ran.
     *
     * @throws Exception
     */
    @AfterAll
    public static void destroy() throws Exception {
        client.close();
    }

    /**
     * Pre-test setup.
     */
    @BeforeEach
    public void setup() {
        response = null;
        client = ClientBuilder.newClient();
    }

    /**
     * Post-test cleanup.
     */
    @AfterEach
    public void teardown() {
        response.close();
        client.close();
    }

    /**
     * Test that the application deployed on the Liberty server can be reached.
     */
    @Test
    public void testApplicationResponse() {
        String livenessURL = baseUrl + RESOURCE_ENDPOINT;
        response = this.getResponse(baseUrl + RESOURCE_ENDPOINT);
        this.assertResponse(livenessURL, response);
        String msg = response.readEntity(String.class);
        assertEquals(expectedGreeting, msg);
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
