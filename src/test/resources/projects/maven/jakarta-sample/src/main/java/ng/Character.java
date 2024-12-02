/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package ng;

import java.io.Serializable;

/**
 * This class can be used in testing to impersonate java.lang.Character in order to
 * verify that qualified class names are processed correctly. It is not intended to
 * be complete or functional.
 */
public class Character implements Serializable, Comparable<Character> {

    @Override
    public int compareTo(Character o) {
        return 0;
    }
}
