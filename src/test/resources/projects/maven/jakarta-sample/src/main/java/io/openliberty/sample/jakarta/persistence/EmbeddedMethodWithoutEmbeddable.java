/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Invalid: AddressNotEmbeddable is NOT annotated with @Embeddable
// Diagnostic should fire on the @Embedded property accessor method
@Entity
public class EmbeddedMethodWithoutEmbeddable {

    @Id
    private Long id;

    private AddressNotEmbeddable address;

    @Embedded
    public AddressNotEmbeddable getAddress() {
        return address;
    }

    public void setAddress(AddressNotEmbeddable address) {
        this.address = address;
    }
}
