/*******************************************************************************
* Copyright (c) 2021, 2024 Red Hat Inc. and others.
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
package io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction;


import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.InsertAnnotationAttributeProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;

import java.text.MessageFormat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * QuickFix for inserting attribute of a given annotation.
 *
 * @author Angelo ZERR
 *
 */
public abstract class InsertAnnotationAttributeQuickFix implements IJavaCodeActionParticipant {

	private static final Logger LOGGER = Logger.getLogger(InsertAnnotationAttributeQuickFix.class.getName());

	private static final String CODE_ACTION_LABEL = "Insert ''{0}'' attribute";

	private final String attributeName;

	/**
	 * Constructor for inserting attribute annotation quick fix.
	 *
	 * @param attributeName name list of annotation to insert.
	 */
	public InsertAnnotationAttributeQuickFix(String attributeName) {
		this.attributeName = attributeName;
	}

	@Override
	public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
		ExtendedCodeAction codeAction = new ExtendedCodeAction(getLabel(attributeName));
		codeAction.setRelevance(0);
		codeAction.setKind(CodeActionKind.QuickFix);
		codeAction.setDiagnostics(Arrays.asList(diagnostic));
		codeAction.setData(
				new CodeActionResolveData(context.getUri(), getParticipantId(), context.getParams().getRange(), null,
						context.getParams().isResourceOperationSupported(),
						context.getParams().isCommandConfigurationUpdateSupported(),
						getCodeActionId()));
		return Collections.singletonList(codeAction);
	}


	@Override
	public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
		CodeAction toResolve = context.getUnresolved();
		PsiElement selectedNode = context.getCoveringNode();
		PsiAnnotation annotation = PsiTreeUtil.getParentOfType(selectedNode, PsiAnnotation.class);
		String name = getLabel(attributeName);
		ChangeCorrectionProposal proposal = new InsertAnnotationAttributeProposal(name, context.getCompilationUnit(),
				annotation, 0, context.getSource().getCompilationUnit(), attributeName);

		ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to resolve code action edit for inserting an attribute value");
		return toResolve;
	}

	/**
	 * Returns the id for this code action.
	 *
	 * @return the id for this code action
	 */
	protected abstract MicroProfileCodeActionId getCodeActionId();
	private static String getLabel(String memberName) {
		return MessageFormat.format(CODE_ACTION_LABEL, memberName);
	}

}
