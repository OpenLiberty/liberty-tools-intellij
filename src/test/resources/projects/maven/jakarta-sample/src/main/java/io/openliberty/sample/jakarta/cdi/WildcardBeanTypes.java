/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation.
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
 * Test class for wildcard type validation in CDI bean types.
 * According to CDI 3.0 specification section 2.2.1:
 * "A parameterized type that contains a wildcard type parameter is not a legal bean type."
 */
public class WildcardBeanTypes {

    // Invalid @Inject fields with wildcard types
    @Inject
    private List<?> wildcardList; // ERROR: wildcard type in @Inject field

    @Inject
    private List<? extends Number> extendsWildcardList; // ERROR: wildcard type in @Inject field

    @Inject
    private List<? super Integer> superWildcardList; // ERROR: wildcard type in @Inject field

    @Inject
    private Map<String, ?> wildcardMap; // ERROR: wildcard type in @Inject field

    // Invalid @Produces fields with wildcard types
    @Produces
    private List<?> producedWildcardList; // ERROR: wildcard type in @Produces field

    @Produces
    private List<? extends Number> producedExtendsWildcardList; // ERROR: wildcard type in @Produces field

    @Produces
    private List<? super Integer> producedSuperWildcardList; // ERROR: wildcard type in @Produces field

    // Invalid @Produces methods with wildcard return types
    @Produces
    public List<?> produceWildcardList() { // ERROR: wildcard type in @Produces method
        return null;
    }

    @Produces
    public List<? extends Number> produceExtendsWildcardList() { // ERROR: wildcard type in @Produces method
        return null;
    }

    @Produces
    public List<? super Integer> produceSuperWildcardList() { // ERROR: wildcard type in @Produces method
        return null;
    }

    // Valid fields and methods (no wildcards)
    @Inject
    private List<String> validList; // OK: no wildcard

    @Produces
    private List<Integer> producedValidList = null; // OK: no wildcard

    @Produces
    public Map<String, Integer> produceValidMap() { // OK: no wildcard
        return null;
    }

    @Inject
    private String simpleType; // OK: not a parameterized type

    // Nested wildcard types - testing recursive wildcard detection
    
    // Invalid @Inject fields with nested wildcard types
    @Inject
    private Map<String, List<?>> nestedMapListWildcard; // ERROR: nested wildcard in @Inject field

    @Inject
    private Map<String, Map<Integer, ?>> nestedMapMapWildcard; // ERROR: nested wildcard in @Inject field

    @Inject
    private List<?>[] arrayWildcard; // ERROR: array of wildcard type in @Inject field

    // Invalid @Produces fields with nested wildcard types
    @Produces
    private Map<String, List<?>> producedNestedMapListWildcard; // ERROR: nested wildcard in @Produces field

    @Produces
    private Map<String, Map<Integer, ?>> producedNestedMapMapWildcard; // ERROR: nested wildcard in @Produces field

    @Produces
    private List<?>[] producedArrayWildcard; // ERROR: array of wildcard type in @Produces field

    // Invalid @Produces methods with nested wildcard return types
    @Produces
    public Map<String, List<?>> produceNestedMapListWildcard() { // ERROR: nested wildcard in @Produces method
        return null;
    }

    @Produces
    public Map<String, Map<Integer, ?>> produceNestedMapMapWildcard() { // ERROR: nested wildcard in @Produces method
        return null;
    }

    // Invalid @Inject methods with wildcard parameter types
    @Inject
    public void setWildcardList(List<?> list) { // ERROR: wildcard type in @Inject method parameter
    }

    @Inject
    public void setExtendsWildcardList(List<? extends Number> list) { // ERROR: wildcard type in @Inject method parameter
    }

    @Inject
    public void setSuperWildcardList(List<? super Integer> list) { // ERROR: wildcard type in @Inject method parameter
    }

    @Inject
    public void setWildcardMap(Map<String, ?> map) { // ERROR: wildcard type in @Inject method parameter
    }

    @Inject
    public void setNestedWildcard(Map<String, List<?>> map) { // ERROR: nested wildcard in @Inject method parameter
    }

    @Inject
    public void setArrayWildcard(List<?>[] array) { // ERROR: array of wildcard type in @Inject method parameter
    }

    @Inject
    public void setMultiDimensionalArrayWildcard(List<?>[][] array) { // ERROR: multi-dimensional array of wildcard type in @Inject method parameter
    }

    // Valid @Inject methods (no wildcards)
    @Inject
    public void setValidList(List<String> list) { // OK: no wildcard
    }

    @Inject
    public void setValidMap(Map<String, Integer> map) { // OK: no wildcard
    }

    // Multiple parameters - only the wildcard ones should be flagged
    @Inject
    public void setMixedParameters(List<String> validList, List<?> wildcardList) { // ERROR: second parameter has wildcard
    }
}