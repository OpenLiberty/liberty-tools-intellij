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
*     IBM Corporation, Archana Iyer R - initial API and implementation
*******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.interceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.helpers.ConstructorInfoDiagnosticHelper;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.interceptor.Constants.*;


/**
 * Interceptor diagnostic participant that manages the use of @Interceptor annotation.
 */
public class InterceptorDiagnosticsParticipant extends AbstractDiagnosticsCollector {

	public InterceptorDiagnosticsParticipant() {
		super();
	}

	@Override
	protected String getDiagnosticSource() {
		return DIAGNOSTIC_SOURCE;
	}

    @Override
	public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
		if (unit == null)
			return;

		PsiClass[] alltypes;
		alltypes = unit.getClasses();
		for (PsiClass type : alltypes) {
			//Build the diagnostics if the parent class is Interceptor type and is abstract.
			// Also, checks for missing public no-args constructor.
        	buildAbstractAndNoArgsConstructorDiagnostics(unit, diagnostics, type);
			for(PsiClass innerClass: type.getInnerClasses()){
				//Build the diagnostics if the child class is Interceptor type and is abstract.
				// Also, checks for missing public no-args constructor.
				buildAbstractAndNoArgsConstructorDiagnostics(unit, diagnostics, innerClass);
			}
        }
    }

	/**
	 * buildAbstractAndNoArgsConstructorDiagnostics
	 * Method checks if the parent or inner classes are Interceptor type and throws appropriate diagnostics
	 *
	 * @param unit
	 * @param diagnostics
	 * @param type
	 */
	private void buildAbstractAndNoArgsConstructorDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiClass type) {
		ConstructorInfoDiagnosticHelper constructorInfo = ConstructorInfoDiagnosticHelper.initialize();
		if (isInterceptorType(type)) {
			if(type.hasModifierProperty(PsiModifier.ABSTRACT)) {
				diagnostics.add(createDiagnostic(type, unit,
						Messages.getMessage("InvalidInterceptorAbstractClass", type.getName()),
						DIAGNOSTIC_CODE_INTERCEPTOR_ON_ABSTRACT_CLASS, null,
						DiagnosticSeverity.Error));
			}
			for (PsiMethod method : type.getMethods()) {
				//Checks if method is a constructor and has valid no-args constructor
				constructorInfo.mergeConstructorInfo(ConstructorInfoDiagnosticHelper.getConstructorInfo(method));
			}
			// Conditions for checking missing public no-args constructor
			if(constructorInfo != null && !constructorInfo.hasValidPublicNoArgsConstructor()
					&& constructorInfo.hasConstructor()) {
				diagnostics.add(createDiagnostic(type, unit,
						Messages.getMessage("InterceptorNoArgConstructorMissing", type.getName()),
						DIAGNOSTIC_CODE_INTERCEPTOR_ON_NO_ARGS_CONSTRUCTOR, null,
						DiagnosticSeverity.Error));
			}
		}
	}

	/**
	 * isInterceptorType
	 * Method checks if the class is an Interceptor type
	 *
	 * @param type
	 * @return boolean
	 */
	private static boolean isInterceptorType(PsiClass type) {
		return Arrays.stream(type.getAnnotations()).filter(Objects::nonNull).anyMatch(annotation -> isMatchedJavaElement(type, annotation.getQualifiedName(), INTERCEPTOR_FQ_NAME));
	}
}