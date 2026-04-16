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
import java.util.Map;
import java.util.List;
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
     *
     * @param unit             compilation unit of Java class.
     * @param annotation       given annotation object.
     * @param annotationFQName the fully qualified annotation name.
     * @return true if the given annotation matches the given annotation name and
     *         false otherwise.
     */
    protected static boolean isMatchedAnnotation(PsiAnnotation annotation, String annotationFQName) {
        String elementName = annotation.getQualifiedName();
        return annotationFQName.equals(elementName);
    }

    /**
     * Returns true if the java element name matches the given fully qualified java
     * element name and false otherwise.
     *
     * @param unit             compilation unit of Java class.
     * @param annotation       given annotation object.
     * @param annotationFQName the fully qualified annotation name.
     * @return true if the java element name matches the given fully qualified java
     *         element name and false otherwise.
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
     * @param javaElement        Java element name
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
     * isImportReferencedJavaElement
     * Method checks if class has references of interceptor type imports
     *
     * @param javaFile
     * @param interceptorImport
     * @return
     */
    private static boolean isImportReferencedJavaElement(PsiJavaFile javaFile, String interceptorImport) {
        PsiImportList importList = javaFile.getImportList();
        if (importList != null) {
            for (PsiImportStatement importStatement : importList.getImportStatements()) {
                PsiElement resolved = importStatement.resolve();
                if (resolved instanceof PsiClass psiClass) {
                    String qualifiedName = psiClass.getQualifiedName();
                    if (qualifiedName != null && qualifiedName.contains(interceptorImport)) {
                       return true;
                    }
                }
                else if (resolved instanceof PsiPackage psiPackage) {
                    String pkgName = psiPackage.getQualifiedName();
                    if (pkgName.contains(interceptorImport)) {
                       return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * isInterceptorType
     * Method checks if the class is an Interceptor type
     *
     * @param type
     * @return boolean
     */
    public static boolean isInterceptorType(PsiClass type) {
        return Arrays.stream(type.getAnnotations()).anyMatch(annotation -> isMatchedJavaElement(type, annotation.getQualifiedName(), Constants.INTERCEPTOR_FQ_NAME));
    }

    /**
     * isInterceptorTypeReferenced
     * Method checks if class references Interceptor types
     *
     * @param type
     * @param javaFile
     * @return
     */
    public static boolean isInterceptorTypeReferenced(PsiClass type, PsiJavaFile javaFile) {
        if(!isInterceptorType(type)){
            return isImportReferencedJavaElement(javaFile, Constants.INTERCEPTOR_IMPORT);
        }
        return false;
    }

    /**
     * checkMethodInvokedExists
     * This method checks if the passed method is being invoked in the declared method body
     *
     * @param m
     * @return
     */
    public boolean checkMethodInvokedExists(PsiMethod m, String methodInvoked, String methodParentType) {
        PsiCodeBlock body = m.getBody();
        if (body != null) {
            Collection<PsiMethodCallExpression> allInterceptorMethodInvocations =
                    PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression.class);
            for (PsiMethodCallExpression call : allInterceptorMethodInvocations) {
                PsiReferenceExpression methodExpr = call.getMethodExpression();
                String methodName = methodExpr.getReferenceName();
                if (methodInvoked.equals(methodName)) {
                    PsiMethod target = call.resolveMethod();
                    if (target != null) {
                        PsiClass containingClass = target.getContainingClass();
                        if (containingClass != null) {
                            String fqn = containingClass.getQualifiedName();
                            if (methodParentType.equals(fqn)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}