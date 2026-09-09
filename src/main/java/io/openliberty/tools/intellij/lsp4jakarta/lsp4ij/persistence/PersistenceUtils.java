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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import com.intellij.psi.PsiClass;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.SourceClassScanner;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for Jakarta Persistence diagnostic collectors.
 *
 * <p>Provides shared helpers used across persistence diagnostic collectors,
 * such as module-wide entity class scanning.
 */
public class PersistenceUtils {

    private PersistenceUtils() {
        // utility class — not instantiable
    }

    /**
     * Scans all source classes in the module that owns {@code context} and returns
     * a map from simple class name to {@link PsiClass} for every class annotated
     * with {@code @jakarta.persistence.Entity}.
     *
     * @param context any {@link PsiClass} from the module (used to locate it)
     * @return a map from simple entity class name to its {@link PsiClass}
     */
    public static Map<String, PsiClass> findAnnotatedEntityClasses(PsiClass context) {
        Map<String, PsiClass> entityTypeMap = new HashMap<>();
        SourceClassScanner.scanSourceClasses(context, scannedClass -> {
            if (AnnotationUtils.getAnnotation(scannedClass, PersistenceConstants.ENTITY) != null
                    && scannedClass.getName() != null) {
                entityTypeMap.put(scannedClass.getName(), scannedClass);
            }
        });
        return entityTypeMap;
    }
}
