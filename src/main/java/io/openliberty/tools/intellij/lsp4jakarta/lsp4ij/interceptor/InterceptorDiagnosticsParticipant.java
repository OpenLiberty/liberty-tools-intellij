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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PositionUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ASTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.helpers.ConstructorInfoDiagnosticHelper;
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
			if (isInterceptorTypeReferenced(type)) {
				//Build the diagnostics if the parent class is Interceptor type and is abstract.
				// Also, checks for missing public no-args constructor.
				validateAbstractClassAndNoArgsConstructor(unit, diagnostics, type);
				for (PsiClass innerClass : type.getInnerClasses()) {
					//Build the diagnostics if the child class is Interceptor type and is abstract.
					// Also, checks for missing public no-args constructor.
					validateAbstractClassAndNoArgsConstructor(unit, diagnostics, innerClass);
				}
				PsiMethod[] allMethods = type.getMethods();
				for (PsiMethod method : allMethods) {
					List<String> interceptorTypeMethodAnnotations = containsAnyMatchingAnnotations(type, method, Constants.INTERCEPTOR_METHODS);
					boolean isFinal = method.hasModifierProperty(PsiModifier.FINAL);
					boolean isAbstract = method.hasModifierProperty(PsiModifier.ABSTRACT);
					boolean isStatic = method.hasModifierProperty(PsiModifier.STATIC);
					String msg;
					DiagnosticSeverity severity = null;
					if (isFinal) {
						addInvalidModifierDiagnostic(method, unit, diagnostics, interceptorTypeMethodAnnotations,
								"InvalidInterceptorMethodAnnotationFinalMethod", DIAGNOSTIC_CODE_INTERCEPTOR_FINAL,
								DiagnosticSeverity.Error);
					}
					if (isAbstract) {
						addInvalidModifierDiagnostic(method, unit, diagnostics, interceptorTypeMethodAnnotations,
								"InvalidInterceptorMethodAnnotationAbstractMethod", DIAGNOSTIC_CODE_INTERCEPTOR_ABSTRACT,
								DiagnosticSeverity.Error);
					}
					if (isStatic) {
						boolean isLifecycleCallback = !containsAnyMatchingAnnotations(type, method, LIFECYCLE_CALLBACK_INTERCEPTOR_METHODS).isEmpty();
						String messageKey = isLifecycleCallback
								? "InvalidLifecycleCallbackMethodAnnotationStaticMethod"
								: "InvalidInterceptorMethodAnnotationStaticMethod";
						severity = isLifecycleCallback ? DiagnosticSeverity.Warning : DiagnosticSeverity.Error;
						addInvalidModifierDiagnostic(method, unit, diagnostics, interceptorTypeMethodAnnotations,
								messageKey, DIAGNOSTIC_CODE_INTERCEPTOR_STATIC, severity);
					}
				}
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
		if(isInterceptorTypeReferenced(method.getContainingClass())) {
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
	 * validateAbstractClassAndNoArgsConstructor
	 * Method checks if the parent or inner classes are Interceptor type and throws appropriate diagnostics
	 *
	 * @param unit
	 * @param diagnostics
	 * @param type
	 */
	private void validateAbstractClassAndNoArgsConstructor(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiClass type) {
		ConstructorInfoDiagnosticHelper constructorInfo = ConstructorInfoDiagnosticHelper.initialize();
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

	/**
	 * Adds a diagnostic for an invalid method modifier on an interceptor method.
	 *
	 * @param method                          the method with the invalid modifier
	 * @param unit                            the compilation unit
	 * @param diagnostics                     the list to add the diagnostic to
	 * @param interceptorTypeMethodAnnotations the list of interceptor annotation FQNs
	 * @param messageKey                      the message key for the diagnostic message
	 * @param diagnosticCode                  the diagnostic code
	 * @param severity                        the diagnostic severity
	 */
	private void addInvalidModifierDiagnostic(PsiMethod method, PsiJavaFile unit, List<Diagnostic> diagnostics,
											   List<String> interceptorTypeMethodAnnotations, String messageKey,
											   String diagnosticCode, DiagnosticSeverity severity) {
		String msg = Messages.getMessage(messageKey, getSimpleAnnotationNames(interceptorTypeMethodAnnotations));
		JsonArray annotationData = (JsonArray) new Gson().toJsonTree(interceptorTypeMethodAnnotations);
		diagnostics.add(createDiagnostic(method, unit, msg, diagnosticCode, annotationData, severity));
	}

	/**
	 * Converts a list of fully qualified annotation names to a comma-separated string of simple names.
	 * Duplicate simple names are removed.
	 *
	 * @param annotations the list of fully qualified annotation names
	 * @return a comma-separated string of simple annotation names
	 */
	private String getSimpleAnnotationNames(List<String> annotations) {
		List<String> simpleAnnotationNames = annotations.stream().map(name -> JDTUtils.getSimpleName(name)).distinct().collect(Collectors.toList());
		return String.join(", ", simpleAnnotationNames);
	}
}