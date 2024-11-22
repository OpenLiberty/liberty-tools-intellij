/*******************************************************************************
* Copyright (c) 2020, 2024 Red Hat Inc. and others.
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
package io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.health.java;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.ExtendedCodeAction;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ImplementInterfaceProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.health.MicroProfileHealthConstants;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * QuickFix for fixing {@link MicroProfileHealthErrorCode#ImplementHealthCheck}
 * error by providing the code actions which implements
 * 'org.eclipse.microprofile.health.HealthCheck'.
 *
 * @author Angelo ZERR
 *
 */
public class ImplementHealthCheckQuickFix implements IJavaCodeActionParticipant {

	private static final Logger LOGGER = Logger.getLogger(ImplementHealthCheckQuickFix.class.getName());

	@Override
	public String getParticipantId() {
		return ImplementHealthCheckQuickFix.class.getName();
	}

	@Override
	public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
		PsiNamedElement binding = getBinding(context.getCoveringNode());
		ExtendedCodeAction codeAction = new ExtendedCodeAction("Let '" + binding.getName() + "' implement '@"
				+ MicroProfileHealthConstants.HEALTH_CHECK_INTERFACE_NAME + "'");
		codeAction.setRelevance(0);
		codeAction.setDiagnostics(Collections.singletonList(diagnostic));
		codeAction.setKind(CodeActionKind.QuickFix);
		codeAction.setData(new CodeActionResolveData(context.getUri(), getParticipantId(),
				context.getParams().getRange(), null, context.getParams().isResourceOperationSupported(),
				context.getParams().isCommandConfigurationUpdateSupported(),
				MicroProfileCodeActionId.ImplementHealthCheck));

		return Collections.singletonList(codeAction);
	}

	@Override
	public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
		CodeAction toResolve = context.getUnresolved();

		PsiElement node = context.getCoveringNode();
		PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
		if (parentType != null) {
			// Create code action to implement 'org.eclipse.microprofile.health.HealthCheck'
			// interface
			ChangeCorrectionProposal proposal = new ImplementInterfaceProposal(context.getCompilationUnit(), parentType,
					context.getASTRoot(), MicroProfileHealthConstants.HEALTH_CHECK_INTERFACE, 0,
					context.getSource().getCompilationUnit());

			ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit to make the class implement @HealthCheck");
		}
		return context.getUnresolved();
	}

	private static PsiNamedElement getBinding(PsiElement node) {
		PsiNamedElement binding = PsiTreeUtil.getParentOfType(node, PsiVariable.class);
		if (binding == null) {
			return PsiTreeUtil.getParentOfType(node, PsiClass.class);
		}
		return binding;
	}

}
