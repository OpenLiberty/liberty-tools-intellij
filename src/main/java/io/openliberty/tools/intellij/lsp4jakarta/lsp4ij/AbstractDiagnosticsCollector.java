/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.interceptor.Constants;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

/**
 *
 * Abstract class for collecting Java diagnostics.
 *
 */
public abstract class AbstractDiagnosticsCollector implements DiagnosticsCollector, IJavaDiagnosticsParticipant {

    /**
     * Constructor
     */
    public AbstractDiagnosticsCollector() {
        super();
    }

    protected String getDiagnosticSource() {
        return null;
    }

    public void completeDiagnostic(Diagnostic diagnostic) {
        this.completeDiagnostic(diagnostic, null, DiagnosticSeverity.Error);
    }

    public void completeDiagnostic(Diagnostic diagnostic, String code) {
        this.completeDiagnostic(diagnostic, code, DiagnosticSeverity.Error);
    }

    public void completeDiagnostic(Diagnostic diagnostic, String code, DiagnosticSeverity severity) {
        String source = getDiagnosticSource();
        if (source != null)
            diagnostic.setSource(source);
        if (code != null)
            diagnostic.setCode(code);
        diagnostic.setSeverity(severity);
    }

    /**
     * Creates and returns a new diagnostic.
     *
     * @param el       Java element
     * @param unit     compilation unit of Java class
     * @param msg      diagnostic message
     * @param code     diagnostic code
     * @param data     diagnostic data
     * @param severity diagnostic severity
     * @return new Diagnostic object.
     */
    protected Diagnostic createDiagnostic(PsiElement el, PsiJavaFile unit, String msg, String code, Object data,
                                          DiagnosticSeverity severity) {
        Range range = PositionUtils.toNameRange(el);
        Diagnostic diagnostic = new Diagnostic(range, msg);
        if (data != null)
            diagnostic.setData(data);
        String source = getDiagnosticSource();
        if (source != null)
            diagnostic.setSource(source);
        if (code != null)
            diagnostic.setCode(code);
        diagnostic.setSeverity(severity);
        return diagnostic;
    }

    /**
     * Collect diagnostics according to the context.
     *
     * @param context the java diagnostics context
     *
     * @return diagnostics list and null otherwise.
     *
     */
    @Override
    public final List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
        PsiFile typeRoot = context.getTypeRoot();
        if (typeRoot instanceof PsiJavaFile) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            collectDiagnostics((PsiJavaFile) typeRoot, diagnostics);
            return diagnostics;
        }
        return Collections.emptyList();
    }

    /**
     * Returns diagnostics for the given compilation unit.
     *
     * @param unit        compilation unit of Java class
     * @param diagnostics diagnostics for the given compilation unit to return
     */
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
    }

    /**
     * Returns true if the given annotation matches the given annotation name and
     * false otherwise.
     * @param annotation
     * @param annotationFQName
     * @return
     */
    protected static boolean isMatchedAnnotation(PsiAnnotation annotation, String annotationFQName) {
        String elementName = annotation.getQualifiedName();
        return annotationFQName.equals(elementName);
    }

    /**
     * Returns true if the given annotation matches the given annotation name and
     * false otherwise.
     * @param annotations
     * @param annotationFQName
     * @return
     */
    protected static boolean isMatchedAnnotation(PsiAnnotation[] annotations, String annotationFQName) {
        return Arrays.stream(annotations)
                .anyMatch(annotation -> isMatchedAnnotation(annotation, annotationFQName));
    }

    /**
     * Returns true if the java element name matches the given fully qualified java
     * element name and false otherwise.
     * @param type
     * @param javaElementName
     * @param javaElementFQName
     * @return true if the java element name matches the given fully qualified java
     *        element name and false otherwise.
     */
    protected static boolean isMatchedJavaElement(PsiClass type, String javaElementName, String javaElementFQName) {
        if (javaElementFQName.equals(javaElementName)) {
            JavaPsiFacade facade = JavaPsiFacade.getInstance(type.getProject());
            Object o = facade.findClass(javaElementFQName, GlobalSearchScope.allScope(type.getProject()));
            return (o != null);
        }
        return false;
    }

    /**
     * Returns true if the given Java class implements one of the given interfaces
     * and false otherwise.
     *
     * @param type             Java class.
     * @param interfaceFQNames given interfaces with fully qualified name.
     * @return true if the Java class implements one of the given interfaces and
     *         false otherwise.
     */
    protected static boolean doesImplementInterfaces(PsiClass type, String[] interfaceFQNames) {
        PsiClass[] interfaces = type.getInterfaces();

        // should check import statements first for the performance?

        // check super hierarchy
        if (interfaces.length > 0) { // the type implements interface(s)
            //ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
            //IType[] interfaces = typeHierarchy.getAllInterfaces();
            for (PsiClass interfase : interfaces) {
                String fqName = interfase.getQualifiedName();
                for (String iName : interfaceFQNames) {
                    if (fqName.equals(iName)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns matched Java element fully qualified name.
     *
     * @param type               Java class.
     * @param javaElementName        Java element name
     * @param javaElementFQNames given fully qualified name array.
     * @return Matched fully qualified name and null otherwise.
     */
    protected static String getMatchedJavaElementName(PsiClass type, String javaElementName, String[] javaElementFQNames) {
        String[] matches = (String[]) Stream.of(javaElementFQNames)
                .filter(fqName -> nameEndsWith(fqName, javaElementName))
                .toArray(String[]::new);
        if (matches.length > 0) {
            if (isMatchedJavaElement(type, javaElementName, matches[0]) == true) // only check the first one for now
                return matches[0];
        }
        return null;
    }

    /**
     * Returns matched Java element fully qualified names.
     *
     * @param type               the type representing the class
     * @param javaElementNames   Java element names
     * @param javaElementFQNames given fully qualified name array
     * @return matched Java element fully qualified names
     */
    protected static List<String> getMatchedJavaElementNames(PsiClass type, String[] javaElementNames,
                                                             String[] javaElementFQNames) {
        return Stream.of(javaElementFQNames).filter(fqName -> {
            boolean anyMatch = Stream.of(javaElementNames).anyMatch(name -> {
                return isMatchedJavaElement(type, name, fqName);
            });
            return anyMatch;
        }).collect(Collectors.toList());
    }

    /**
     * Returns true if the given fully qualified name ends with the given name and
     * false otherwise
     *
     * @param fqName fully qualified name
     * @param name   either simple name or fully qualified name
     * @return true if the given fully qualified name ends with the given name and
     *         false otherwise
     */
    protected static boolean nameEndsWith(String fqName, String name) {
        // add a prefix '.' to simple name
        // e.g. 'jakarta.validation.constraints.DecimalMin' should NOT end with 'Min'
        // here
        return fqName.equals(name) || fqName.endsWith("." + name);
    }

    /**
     * Returns true if the given method is a constructor and false otherwise.
     *
     * @param m method
     * @return true if the given method is a constructor and false otherwise
     */
    public static boolean isConstructorMethod(PsiMethod m) {
        return m.isConstructor();
    }

    /**
     * isInterceptorType
     * Method checks if the class is an Interceptor type
     *
     * @param type the type to check
     * @return true if the type has @Interceptor annotation
     */
    public static boolean isInterceptorType(PsiClass type) {
        return Arrays.stream(type.getAnnotations())
                .anyMatch(annotation -> isMatchedJavaElement(type, annotation.getQualifiedName(), Constants.INTERCEPTOR_FQ_NAME));
    }

    /**
     * Checks if type is of Interceptor type or uses interceptor-related features.
     * Returns true if:
     * - The type has @Interceptor annotation
     * - The type or its methods use interceptor-specific annotations (AroundInvoke, AroundConstruct, AroundTimeout)
     * - The type or its methods use @Interceptors annotation
     *
     * Note: This excludes PostConstruct and PreDestroy as they belong to the annotations module.
     *
     * @param type     the type to check
     * @return true if the type is an interceptor type or uses interceptor-related features
     */
    public static boolean isInterceptorTypeReferenced(PsiClass type) {
        if (type == null) {
            return false;
        }

        // Check if the type has @Interceptor annotation (no method iteration needed)
        if (isInterceptorType(type)) {
            return true;
        }

        PsiMethod[] methods = type.getMethods();

        // Check if the type or methods use interceptor-specific annotations
        if (hasInterceptorMethodAnnotations(type, methods)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the type has any methods annotated with interceptor-specific annotations.
     * Checks for: @AroundInvoke, @AroundConstruct, @AroundTimeout
     *
     * @param type    the type to check
     * @param methods the methods array (pre-fetched to avoid redundant calls)
     * @return true if any method uses interceptor-specific annotations
     */
    private static boolean hasInterceptorMethodAnnotations(PsiClass type, PsiMethod[] methods) {
        String[] interceptorReferences = Constants.INTERCEPTOR_REFERENCES.toArray(String[]::new);

        return Arrays.stream(methods)
                .flatMap(method -> Arrays.stream(method.getAnnotations()))
                .anyMatch(annotation -> {
                    String annotationName = annotation.getQualifiedName();
                    return getMatchedJavaElementName(type, annotationName, interceptorReferences) != null;
                });
    }

    /**
     * checkMethodInvokedExists
     * This method checks if the passed method is being invoked in the declared method body
     *
     * @param psiMethod the method to check
     * @param methodInvoked the name of the method to search for
     * @param methodParentType the fully qualified name of the parent type containing the invoked method
     * @return true if the specified method invocation exists in the method body, false otherwise
     */
    public boolean checkMethodInvokedExists(PsiMethod psiMethod, String methodInvoked, String methodParentType) {
        PsiCodeBlock body = psiMethod.getBody();
        if (body == null) {
            return false;
        }
        
        Collection<PsiMethodCallExpression> allInterceptorMethodInvocations =
                PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression.class);
        
        for (PsiMethodCallExpression call : allInterceptorMethodInvocations) {
            if (isMatchingMethodInvocation(call, methodInvoked, methodParentType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a method call expression matches the expected method name and parent type.
     *
     * @param call the method call expression to check
     * @param methodInvoked the name of the method to match
     * @param methodTargetClass the fully qualified name of the parent type to match
     * @return true if the method call matches both the method name and parent type, false otherwise
     */
    private boolean isMatchingMethodInvocation(PsiMethodCallExpression call,
                                              String methodInvoked,
                                              String methodTargetClass) {
        PsiReferenceExpression methodExpr = call.getMethodExpression();
        String methodName = methodExpr.getReferenceName();
        
        if (!methodInvoked.equals(methodName)) {
            return false;
        }
        
        PsiMethod target = call.resolveMethod();
        if (target == null) {
            return false;
        }
        
        PsiClass containingClass = target.getContainingClass();
        if (containingClass == null) {
            return false;
        }
        
        String fqn = containingClass.getQualifiedName();
        return methodTargetClass.equals(fqn);
    }

    /**
     * Checks if a method contains any annotations that match the provided fully qualified annotation names.
     * Returns a list of matched annotation FQNs found on the method.
     *
     * @param type              the class containing the method
     * @param method            the method to check for annotations
     * @param annotationFQNames the set of fully qualified annotation names to match against
     * @return a list of matched fully qualified annotation names, or an empty list if no matches found
     */
    public List<String> containsAnyMatchingAnnotations(PsiClass type, PsiMethod method, Set<String> annotationFQNames) {
        List<String> matchedAnnotations = Arrays.stream(method.getAnnotations())
                .map(annotation -> getMatchedJavaElementName(
                        type,
                        annotation.getQualifiedName(),
                        annotationFQNames.toArray(String[]::new)
                ))
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return matchedAnnotations;
    }
}