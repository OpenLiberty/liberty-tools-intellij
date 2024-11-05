/* Copyright (c) 2022, 2024 IBM Corporation, Lidia Ataupillco Ramos and others.
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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.psi.PsiClass;
import io.openliberty.tools.intellij.util.ExceptionUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Returns the list of recognised defining annotations applied to a
 * class.
 *
 * @param type the type representing the class
 * @param scopes list of defining annotations
 * @return list of recognised defining annotations applied to a class
 */
public class AnnotationUtil {
    private static final Logger LOGGER = Logger.getLogger(AnnotationUtil.class.getName());
    public static List<String> getScopeAnnotations(PsiClass type, Set<String> scopes) {
        return ExceptionUtil.executeWithExceptionHandling(
                () -> Arrays.stream(type.getAnnotations())
                        .map(annotation -> annotation.getNameReferenceElement().getQualifiedName())
                        .filter(scopes::contains)
                        .distinct()
                        .collect(Collectors.toList()),
                e -> {
                    LOGGER.log(Level.WARNING, "Error while calling getScopeAnnotations", e);
                    return Collections.<String>emptyList();
                }
        );
    }
}
