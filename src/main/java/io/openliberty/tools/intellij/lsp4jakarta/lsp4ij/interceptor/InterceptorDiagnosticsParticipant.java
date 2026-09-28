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

import java.util.List;
import java.util.stream.Collectors;
import java.util.Collection;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PositionUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Range;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.helpers.ConstructorInfoDiagnosticHelper;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ASTUtils;
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
		Collection<PsiMethod> allMethodDeclarations = ASTUtils.getAllMethodDeclarations(unit);
		List<PsiMethod> methodsMissingProceedInvocation = allMethodDeclarations.stream().filter(m -> missingInterceptorMethodProceedInvocation(m, unit)).collect(Collectors.toList());
		for(PsiMethod invokeMethod: methodsMissingProceedInvocation){
			Range range = PositionUtils.toNameRange(invokeMethod);
			Diagnostic diagnostic = new Diagnostic(range, Messages.getMessage("InvalidInterceptorMethodsProceedMissing"));
			completeDiagnostic(diagnostic, Constants.DIAGNOSTIC_CODE_INTERCEPTOR_METHOD_MISSING_PROCEED);
			diagnostics.add(diagnostic);
		}
    }

	/**
	 * Checks if an interceptor method is missing the required proceed() invocation.
	 *
	 * This method verifies that interceptor methods (annotated with @AroundInvoke,
	 * @AroundConstruct, @PostConstruct, @PreDestroy, or @AroundTimeout) properly
	 * invoke the proceed() method on the InvocationContext parameter.
	 *
	 * @param method the method to check for proceed invocation
	 * @param unit the Java file containing the method
	 * @return true if the method is an interceptor method missing proceed() invocation, false otherwise
	 */
	private boolean missingInterceptorMethodProceedInvocation(PsiMethod method, PsiJavaFile unit) {
		if(isInterceptorTypeReferenced(method.getContainingClass(), unit)) {
			PsiAnnotation[] annotations = method.getModifierList().getAnnotations();
			for (PsiAnnotation ann : annotations) {
				boolean isInterceptorMethod = Constants.INTERCEPTOR_METHODS.stream().anyMatch(annotation -> isMatchedJavaElement(method.getContainingClass(), ann.getQualifiedName(), annotation));
				if (isInterceptorMethod) {
					// Check if the interceptor method invokes proceed() on InvocationContext
					return !checkMethodInvokedExists(method, Constants.PROCEED, Constants.JAKARTA_INTERCEPTOR_INVOCATION_CONTEXT);
				}
			}
		}
		return false;
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
			} else {
				for (PsiMethod method : type.getMethods()) {
					//Checks if method is a constructor and has valid no-args constructor
					constructorInfo.mergeConstructorInfo(ConstructorInfoDiagnosticHelper.getConstructorInfo(method));
				}
				// Conditions for checking missing public no-args constructor
				if (constructorInfo.hasConstructor() && !constructorInfo.hasValidPublicNoArgsConstructor()) {
					diagnostics.add(createDiagnostic(type, unit,
							Messages.getMessage("InterceptorNoArgConstructorMissing", type.getName()),
							DIAGNOSTIC_CODE_INTERCEPTOR_ON_NO_ARGS_CONSTRUCTOR, null,
							DiagnosticSeverity.Error));
				}
			}
		}
	}
}