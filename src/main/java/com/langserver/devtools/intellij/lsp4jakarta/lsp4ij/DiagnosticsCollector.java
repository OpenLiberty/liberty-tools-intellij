/*******************************************************************************
* Copyright (c) 2020 IBM Corporation, Pengyu Xiong and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation, Pengyu Xiong - initial API and implementation
*******************************************************************************/

package com.langserver.devtools.intellij.lsp4jakarta.lsp4ij;

import java.util.List;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import org.eclipse.lsp4j.Diagnostic;

/**
 * Diagnostics Collector interface
 * @author Pengyu Xiong
 *
 */
public interface DiagnosticsCollector {
    public void completeDiagnostic(Diagnostic diagnostic);

    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics);
}
