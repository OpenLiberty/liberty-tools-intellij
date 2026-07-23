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

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Embeddable
class AddressEmbeddable {
    private String street;
    private String city;
}

// Valid: AddressEmbeddable is annotated with @Embeddable — no diagnostic expected
@Entity
public class EmbeddedWithEmbeddable {

    @Id
    private Long id;

    @Embedded
    private AddressEmbeddable address;
}
