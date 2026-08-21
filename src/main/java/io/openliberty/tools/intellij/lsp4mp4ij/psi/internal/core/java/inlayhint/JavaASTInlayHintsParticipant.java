/*******************************************************************************
* Copyright (c) 2025 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.java.inlayhint;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiFile;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.inlayhint.IJavaInlayHintsParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.inlayhint.JavaInlayHintsContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.java.validators.MultiASTVisitor;

import java.util.Collection;

/**
 * The java inlay hints participant which visit one time a given AST compilation
 * unit and loops for each {@link JavaASTInlayHint} registered with
 * "org.eclipse.lsp4mp.jdt.core.javaASTInlayHints" extension point to generate
 * LSP inlay hints.
 *
 * @author Angelo ZERR
 *
 */
public class JavaASTInlayHintsParticipant implements IJavaInlayHintsParticipant {

	@Override
	public void collectInlayHint(JavaInlayHintsContext context, ProgressIndicator monitor) {
		// Collect the list of JavaASTInlayHint which are adapted for the current AST
		// compilation unit to collect inlay hints.
		Collection<JavaRecursiveElementVisitor> inlayHints = JavaASTInlayHintRegistry.getInstance().getInlayHints(context);
		if (!inlayHints.isEmpty()) {
			// Visit the AST compilation unit and process each inlay hint collector.
			PsiFile ast = context.getASTRoot();
			ast.accept(new MultiASTVisitor(inlayHints));
		}
	}
}
