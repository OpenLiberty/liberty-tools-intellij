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

import java.util.Map;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
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
                checkNegativePriority(unit, diagnostics, type);
                for (PsiClass innerClass : type.getInnerClasses()) {
    				//Build the diagnostics if the child class is Interceptor type and is abstract.
     				// Also, checks for missing public no-args constructor.
    				validateAbstractClassAndNoArgsConstructor(unit, diagnostics, innerClass);
                    checkNegativePriority(unit, diagnostics, innerClass);
                }
				PsiMethod[] allMethods = type.getMethods();
				// Track methods by interceptor annotation type to detect duplicates
				Map<String, List<PsiMethod>> methodsByAnnotationType = new HashMap<>();
				for (PsiMethod method : allMethods) {
					List<String> interceptorTypeMethodAnnotations = detectInterceptorMethodsAndDuplicates(type, method, methodsByAnnotationType);
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
				// Check for duplicate interceptor method annotations
				validateDuplicateInterceptorMethods(methodsByAnnotationType, unit, diagnostics);
				
				// Process inner classes for duplicate interceptor method annotations
				for (PsiClass innerClass : type.getInnerClasses()) {
					if (isInterceptorTypeReferenced(innerClass)) {
						validateDuplicateInterceptorMethodsForClass(innerClass, unit, diagnostics);
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

	/**
	 * Checks if an @Interceptor class has a @Priority annotation with a negative value.
	 * According to the Jakarta Interceptors specification, negative priority values are
	 * reserved for future use and should not be used.
	 *
	 * @param unit the Java file containing the class
	 * @param diagnostics the list to add diagnostics to
	 * @param type the class to check
	 */
	private void checkNegativePriority(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiClass type) {
		if (!isInterceptorType(type)) {
			return;
		}

		// Check if the class has @Priority annotation
		PsiAnnotation priorityAnnotation = null;
		for (PsiAnnotation annotation : type.getAnnotations()) {
			if (isMatchedJavaElement(type, annotation.getQualifiedName(), PRIORITY_FQ_NAME)) {
				priorityAnnotation = annotation;
				break;
			}
		}

		if (priorityAnnotation == null) {
			return;
		}

		if (DiagnosticsUtils.isNegativePriorityValue(priorityAnnotation)) {
			Range range = PositionUtils.toNameRange(priorityAnnotation);
			Diagnostic diagnostic = new Diagnostic(range, Messages.getMessage("InterceptorNegativePriority"));
			completeDiagnostic(diagnostic, DIAGNOSTIC_CODE_INTERCEPTOR_NEGATIVE_PRIORITY, DiagnosticSeverity.Error);
			diagnostics.add(diagnostic);
		}
	}
  
  /**
   * Validates that a class does not have multiple methods with the same interceptor annotation type.
	 * According to Jakarta Interceptors specification, up to one interceptor method of each type may be
	 * defined in the same class.
	 *
	 * @param methodsByAnnotationType map of annotation FQN to list of methods with that annotation
	 * @param unit                    the compilation unit
	 * @param diagnostics             the list to add diagnostics to
	 */
	private void validateDuplicateInterceptorMethods(Map<String, List<PsiMethod>> methodsByAnnotationType,
													  PsiJavaFile unit, List<Diagnostic> diagnostics) {
		// Check for duplicate interceptor method annotations
		for (Entry<String, List<PsiMethod>> entry : methodsByAnnotationType.entrySet()) {
			List<PsiMethod> methods = entry.getValue();
			if (methods.size() > 1) {
				String annotationFQN = entry.getKey();
				String simpleAnnotationName = JDTUtils.getSimpleName(annotationFQN);
				String msg = Messages.getMessage("InvalidMultipleInterceptorMethodsOfSameType", "@" + simpleAnnotationName);
				
				// Report diagnostic for all duplicate methods (skip the first one)
				for (int i = 1; i < methods.size(); i++) {
					PsiMethod method = methods.get(i);
					JsonArray annotationData = (JsonArray) new Gson().toJsonTree(Arrays.asList(annotationFQN));
					diagnostics.add(createDiagnostic(method, unit, msg,
							DIAGNOSTIC_CODE_DUPLICATE_INTERCEPTOR_METHOD, annotationData, DiagnosticSeverity.Error));
				}
			}
		}
	}

	/**
	 * Validates that a class does not have multiple methods with the same interceptor annotation type.
	 * This is a helper method that processes methods for a given class (typically inner classes).
	 *
	 * @param type        the class to validate
	 * @param unit        the compilation unit
	 * @param diagnostics the list to add diagnostics to
	 */
	private void validateDuplicateInterceptorMethodsForClass(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
		PsiMethod[] allMethods = type.getMethods();
		// Create a separate map for this class to avoid mixing with parent class methods
		Map<String, List<PsiMethod>> methodsByAnnotationType = new HashMap<>();
		for (PsiMethod method : allMethods) {
			detectInterceptorMethodsAndDuplicates(type, method, methodsByAnnotationType);
		}
		// Check for duplicate interceptor method annotations
		validateDuplicateInterceptorMethods(methodsByAnnotationType, unit, diagnostics);
	}

	/**
		* Tracks a method by its interceptor annotation types in the provided map.
		* This is used to detect duplicate interceptor methods of the same type.
		*
		* @param type                    the class containing the method
		* @param method                  the method to track
		* @param methodsByAnnotationType the map to track methods by annotation type
		* @return the list of interceptor annotation FQNs found on the method
		*/
	private List<String> detectInterceptorMethodsAndDuplicates(PsiClass type, PsiMethod method,
													  Map<String, List<PsiMethod>> methodsByAnnotationType) {
		List<String> interceptorTypeMethodAnnotations = containsAnyMatchingAnnotations(type, method, Constants.INTERCEPTOR_METHODS);
		// Track methods for duplicate detection
		for (String annotationFQN : interceptorTypeMethodAnnotations) {
			methodsByAnnotationType.computeIfAbsent(annotationFQN, k -> new ArrayList<>()).add(method);
		}
		return interceptorTypeMethodAnnotations;
  }
}