/* Copyright (c) 2022 IBM Corporation, Lidia Ataupillco Ramos and others.
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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiClass;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
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
    public static List<String> getScopeAnnotations(PsiClass type, Set<String> scopes) {
        try {
            // Construct a stream of only the annotations applied to the type that are also
            // recognised annotations found in scopes.
            return Arrays.stream(type.getAnnotations()).map(annotation -> annotation.getNameReferenceElement().getQualifiedName())
                    .filter(scopes::contains).distinct().collect(Collectors.toList());
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            return Collections.<String>emptyList();
        }
    }
}
