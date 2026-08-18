/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/resource")
public class SampleResource {

    @GET
    public String getRequest() {
        return "Hello! Welcome to Open Liberty";
    }
}
