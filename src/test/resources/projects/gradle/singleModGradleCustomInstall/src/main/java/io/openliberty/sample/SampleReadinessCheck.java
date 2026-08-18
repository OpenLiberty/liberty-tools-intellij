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

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;


@Readiness
@ApplicationScoped
public class SampleReadinessCheck implements HealthCheck {

    private boolean isReady() {
        return true;
    }

    @Override
    public HealthCheckResponse call() {
        boolean up = isReady();
        return HealthCheckResponse.named(this.getClass().getSimpleName()).status(up).build();
    }

}
