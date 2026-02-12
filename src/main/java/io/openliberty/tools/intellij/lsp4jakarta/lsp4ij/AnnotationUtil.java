/* Copyright (c) 2022, 2026 IBM Corporation, Lidia Ataupillco Ramos and others.
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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.Jax_RSConstants;
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
                // Construct a stream of only the annotations applied to the type that are also
                // recognised annotations found in scopes.
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

    /**
     * Check existence of meta annotation
     *
     * @param annotation
     * @param type
     * @param metaAnnotationFQN
     * @return Returns true if the annotation has annotated with meta annotation
     */
    public static boolean hasMetaAnnotation(PsiAnnotation annotation, PsiClass type, String metaAnnotationFQN){
        PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
        PsiElement resolved = ref != null ? ref.resolve() : null;

        if (resolved instanceof PsiClass targetClass) {
            return Arrays.stream(targetClass.getAnnotations())
                    .map(PsiAnnotation::getQualifiedName)
                    .anyMatch(qualifiedName ->
                            DiagnosticsUtils.isMatchedJavaElement(type, qualifiedName, metaAnnotationFQN)
                    );
        }
        return false;
    }

}
