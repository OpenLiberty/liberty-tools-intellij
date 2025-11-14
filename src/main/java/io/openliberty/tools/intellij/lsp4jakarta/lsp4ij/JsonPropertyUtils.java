/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb.JsonbConstants;


/**
 * Utilities for working with JSON properties and extracting/decoding values from its attribute, annotation etc.
 */
public class JsonPropertyUtils {

    /**
     * @param propertyName
     * @return String
     * @description Method decodes unicode property name value to string value
     */
    public static String decodeUnicodeName(String propertyName) {
        Pattern pattern = Pattern.compile(JsonbConstants.JSONB_PROPERTYNAME_UNICODE); // Pattern for detecting unicode sequence
        Matcher matcher = pattern.matcher(propertyName);
        StringBuffer decoded = new StringBuffer();
        while (matcher.find()) {
            String unicode = matcher.group(1);
            char decodedChar = (char) Integer.parseInt(unicode, 16);
            matcher.appendReplacement(decoded, Character.toString(decodedChar));
        }
        matcher.appendTail(decoded);
        return decoded.toString();
    }

    /**
     * @param annotation
     * @return String
     * @description Method extracts property name value from the annotation
     */
    public static String extractPropertyNameFromJsonField(PsiAnnotation annotation) {
        PsiAnnotationMemberValue psiValue = annotation.findAttributeValue("value");
        String value = psiValue != null ? psiValue.getText() : null;
        // Remove wrapping quotes if it's a string literal
        if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}