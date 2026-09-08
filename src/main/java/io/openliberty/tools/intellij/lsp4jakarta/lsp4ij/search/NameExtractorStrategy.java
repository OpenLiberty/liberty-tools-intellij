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

import com.intellij.psi.PsiClass;

import java.util.Map;

/**
 * Caller-supplied strategy that decides <em>what</em> to extract from a visited
 * {@link PsiClass} and how to accumulate it into the name-count map.
 *
 * <p>The implementation has full access to everything reachable from the class:
 * <ul>
 *   <li>Class-level annotation attributes (e.g. {@code @NamedEntityGraph(name)})</li>
 *   <li>Field-level annotation attributes (e.g. injection point names)</li>
 *   <li>Method-level annotation attributes</li>
 *   <li>Any combination of the above</li>
 * </ul>
 *
 * <p>Example — collect {@code @NamedEntityGraph} names from class annotations:
 * <pre>{@code
 * NameExtractorStrategy extractor = (psiClass, nameCount) -> {
 *     PsiAnnotation ann = psiClass.getAnnotation(NAMED_ENTITY_GRAPH);
 *     if (ann != null) {
 *         String name = getNameAttr(ann);
 *         if (name != null) nameCount.merge(name, 1, Integer::sum);
 *     }
 * };
 * }</pre>
 *
 * <p>Example — collect {@code @Inject} field names:
 * <pre>{@code
 * NameExtractorStrategy extractor = (psiClass, nameCount) -> {
 *     for (PsiField field : psiClass.getFields()) {
 *         if (field.getAnnotation(INJECT) != null) {
 *             nameCount.merge(field.getName(), 1, Integer::sum);
 *         }
 *     }
 * };
 * }</pre>
 */
@FunctionalInterface
public interface NameExtractorStrategy {

    /**
     * Inspect {@code psiClass} and merge any names of interest into {@code nameCount}.
     *
     * @param psiClass  the class currently being visited by {@link ProjectWideNameScanner}
     * @param nameCount mutable map; use {@code nameCount.merge(name, 1, Integer::sum)}
     */
    void extract(PsiClass psiClass, Map<String, Integer> nameCount);
}
