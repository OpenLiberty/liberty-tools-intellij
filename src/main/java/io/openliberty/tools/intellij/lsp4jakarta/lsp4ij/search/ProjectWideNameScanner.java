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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.search;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds a <em>name → occurrence-count</em> map by visiting every source type
 * in the project using {@link AllClassesSearch}.
 *
 * <p>Only called when {@link JakartaSearchSettings#SEARCH_ENGINE_DIAGNOSTICS_ENABLED}
 * is {@code true}; callers must check that flag before invoking this class.
 *
 * <p><b>Performance note:</b> every diagnostic call pays the full scan cost,
 * regardless of how many types actually carry the elements of interest.
 * With a large source set this cost grows linearly with project size.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * Map<String, Integer> counts = ProjectWideNameScanner.scan(
 *     unit.getProject(),
 *     (psiClass, nameCount) -> {
 *         PsiAnnotation ann = psiClass.getAnnotation(NAMED_ENTITY_GRAPH);
 *         if (ann != null) {
 *             String name = getNameAttr(ann);
 *             if (name != null) nameCount.merge(name, 1, Integer::sum);
 *         }
 *     });
 * }</pre>
 */
public final class ProjectWideNameScanner {

    /**
     * Scans every source type in {@code project} and returns a name → count map.
     *
     * <p>Only call this method after confirming
     * {@link JakartaSearchSettings#SEARCH_ENGINE_DIAGNOSTICS_ENABLED} is {@code true}.
     *
     * @param project   the IntelliJ project
     * @param extractor caller-supplied logic — decides what to collect from each class
     * @return name → occurrence count across all source types in the project
     */
    public static Map<String, Integer> scan(Project project, NameExtractorStrategy extractor) {
        Map<String, Integer> nameCount = new HashMap<>();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

        AllClassesSearch.search(scope, project).forEach(psiClass -> {
            extractor.extract(psiClass, nameCount);
            return true;
        });

        return nameCount;
    }

    private ProjectWideNameScanner() {
        // utility class — no instances
    }
}
