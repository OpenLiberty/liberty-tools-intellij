/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.sample.jakarta.cdi;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Negative test resource for wildcard bean type validation.
 * All @Inject fields, @Inject method parameters, @Produces fields, and
 * @Produces methods use concrete parameterized types without wildcards.
 * No diagnostics should be reported for this class.
 */
public class ValidWildcardBeanTypes {

    // Valid: concrete parameterized types in @Inject fields
    @Inject
    private List<String> concreteList; // OK: no wildcard

    @Inject
    private List<Integer> intList; // OK: no wildcard

    @Inject
    private Map<String, Integer> concreteMap; // OK: no wildcard

    @Inject
    private Map<String, List<Integer>> nestedConcreteMap; // OK: nested but no wildcard

    // Valid: raw type (not a parameterized type — no wildcard check applies)
    @Inject
    private String simpleType; // OK: not a parameterized type

    // Valid: @Produces fields with concrete parameterized types
    @Produces
    private List<String> producerConcreteList = null; // OK: no wildcard

    @Produces
    private Map<String, Integer> producerConcreteMap = null; // OK: no wildcard

    // Valid: @Produces methods with concrete return types
    @Produces
    public List<Double> produceConcreteList() { // OK: no wildcard
        return null;
    }

    @Produces
    public Map<String, Integer> produceConcreteMap() { // OK: no wildcard
        return null;
    }

    @Produces
    public Map<String, List<Number>> produceNestedConcreteMap() { // OK: nested but no wildcard
        return null;
    }

    // Valid: @Inject methods with concrete parameterized parameter types
    @Inject
    public void setConcreteList(List<String> list) { // OK: no wildcard
    }

    @Inject
    public void setConcreteMap(Map<String, Integer> map) { // OK: no wildcard
    }

    @Inject
    public void setMultipleConcreteParams(List<String> list, Map<String, Integer> map) { // OK: no wildcard
    }

    @Inject
    public void setNestedConcreteParam(Map<String, List<Number>> map) { // OK: nested but no wildcard
    }
}
