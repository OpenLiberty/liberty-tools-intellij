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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

import java.util.function.Consumer;

/**
 * Utility class for scanning source classes across an IntelliJ module.
 *
 * <p>Provides a module-wide class visitor facility used by diagnostic collectors
 * that require cross-file analysis.
 */
public class SourceClassScanner {

    private SourceClassScanner() {
        // utility class — not instantiable
    }

    /**
     * Visits every source {@link PsiClass} in the module that owns {@code context}
     * and passes each one to {@code visitor}.
     *
     * <p>Performs a direct filesystem traversal of the module's non-test source roots
     * (via {@link ModuleRootManager}) rather than relying on IntelliJ's annotation
     * index, so it is safe to call during tests before the index is fully built.
     *
     * @param context any {@link PsiClass} from the module (provides module and project)
     * @param visitor called once for every source class found
     */
    public static void scanSourceClasses(PsiClass context, Consumer<PsiClass> visitor) {
        Module module = ModuleUtilCore.findModuleForPsiElement(context);
        if (module == null) {
            return;
        }
        PsiManager psiManager = PsiManager.getInstance(context.getProject());
        for (VirtualFile sourceRoot : ModuleRootManager.getInstance(module).getSourceRoots(false)) {
            visitClasses(sourceRoot, psiManager, visitor);
        }
    }

    /**
     * Recursively visits all {@code .java} files under {@code directory} and
     * passes every {@link PsiClass} found to {@code visitor}.
     *
     * @param directory  the virtual file directory to traverse
     * @param psiManager the PSI manager used to parse virtual files
     * @param visitor    called for each class found
     */
    private static void visitClasses(VirtualFile directory, PsiManager psiManager,
                                     Consumer<PsiClass> visitor) {
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                visitClasses(child, psiManager, visitor);
            } else if ("java".equals(child.getExtension())) {
                PsiFile psiFile = psiManager.findFile(child);
                if (psiFile instanceof PsiJavaFile javaFile) {
                    for (PsiClass psiClass : javaFile.getClasses()) {
                        visitor.accept(psiClass);
                    }
                }
            }
        }
    }
}
