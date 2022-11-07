/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * 	Contributors:
 * 		 Red Hat Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;

import java.util.ArrayList;
import java.util.List;

public class JDTUtils {
    // Percent encoding obtained from: https://en.wikipedia.org/wiki/Percent-encoding#Reserved_characters
    private static final String LEVEL1_URI_REGEX = "(?:\\/(?:(?:\\{(\\w|-|%20|%21|%23|%24|%25|%26|%27|%28|%29|%2A|%2B|%2C|%2F|%3A|%3B|%3D|%3F|%40|%5B|%5D)+\\})|(?:(\\w|%20|%21|%23|%24|%25|%26|%27|%28|%29|%2A|%2B|%2C|%2F|%3A|%3B|%3D|%3F|%40|%5B|%5D)+)))*\\/?";

    /**
     * Check if a URI starts with a leading slash.
     *
     * @param uriString
     * @return boolean
     */
    public static boolean hasLeadingSlash(String uriString) {
        return uriString.startsWith("/");
    }

    /**
     * Check if a URI follows a valid URI-template (level-1) specified by
     * <a href="https://datatracker.ietf.org/doc/html/rfc6570">RFC 6570</a>.
     *
     * @param uriString
     * @return boolean
     */
    public static boolean isValidLevel1URI(String uriString) {
        return uriString.matches(LEVEL1_URI_REGEX);
    }

    /**
     * Returns a list of all accessors (getter and setter) of the given field.
     * Note that for boolean fields the accessor of the form "isField" is retuned
     * "getField" is not present.
     *
     * @param unit      the compilation unit the field belongs to
     * @param field     the accesors of this field are returned
     * @return          a list of accessor methods
     */
    public static List<PsiMethod> getFieldAccessors(PsiJavaFile unit, PsiField field) {
        List<PsiMethod> accessors = new ArrayList<PsiMethod>();
        String fieldName = field.getName();
        fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        List<String> accessorNames = new ArrayList<String>();
        accessorNames.add("get" + fieldName);
        accessorNames.add("set" + fieldName);
        accessorNames.add("is" + fieldName);

        for (PsiClass type : unit.getClasses()) {
            for (PsiMethod method : type.getMethods()) {
                String methodName = method.getName();
                if (accessorNames.contains(methodName))
                    accessors.add(method);
            }
        }
        return accessors;
    }
}
