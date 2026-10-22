/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package io.openliberty.sample.jakarta.cdi;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class BareTypeVariableInjectBean<T> {

    // Invalid: bare type variable T as @Inject field type
    @Inject
    T bareTypeField;

    // Invalid: array of bare type variable T[] as @Inject field type
    @Inject
    T[] bareTypeArrayField;

    // Invalid: bare type variable T as @Inject method parameter
    @Inject
    public void setBareType(T value) {
    }

    // Invalid: array of bare type variable T[] as @Inject method parameter
    @Inject
    public void setBareTypeArray(T[] values) {
    }

    // Valid: parameterized type with type variable (List<T>)
    @Inject
    List<T> genericList;

    // Valid: concrete type — no diagnostic
    @Inject
    String concreteField;
}
