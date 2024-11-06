/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package th;

/**
 * This class can be used in testing to impersonate java.math.BigInteger in order to
 * verify that qualified class names are processed correctly. It is not intended to
 * be complete or functional.
 */
public class BigInteger extends Number implements Comparable<BigInteger> {

    @Override
    public int compareTo(BigInteger o) {
        return 0;
    }

    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 0;
    }

    @Override
    public float floatValue() {
        return 0;
    }

    @Override
    public double doubleValue() {
        return 0;
    }
}
