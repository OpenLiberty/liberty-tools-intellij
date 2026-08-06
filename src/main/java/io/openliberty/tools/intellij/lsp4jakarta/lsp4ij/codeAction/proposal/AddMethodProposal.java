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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PositionUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ASTRewriteCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

import java.util.List;

/**
 * Code action proposal for adding a method to a class.
 *
 * <p>Supports configurable return type, modifier, method-level annotations,
 * and a list of parameters (each optionally wrapping its type in a generic
 * type argument).
 *
 * <p>Used to insert required {@code notify} overrides on custom
 * {@code ObserverMethod} implementations.
 */
public class AddMethodProposal extends ASTRewriteCorrectionProposal {

    /**
     * Describes a single method parameter.
     *
     * <p>A parameter always has a fully-qualified type name and a variable name.
     * When {@code typeArgFQName} is non-{@code null} the type is rendered as a
     * parameterised type: {@code paramTypeFQName<typeArgFQName>}.
     */
    public static class MethodParam {

        /** Fully-qualified type of the parameter (or raw type when generic). */
        public final String typeFQName;

        /** Variable name used in the generated source. */
        public final String name;

        /**
         * Optional fully-qualified type argument that turns {@code typeFQName} into a
         * parameterised type, e.g. {@code EventContext<AuditEvent>}.
         * {@code null} means use {@code typeFQName} as a simple (non-generic) type.
         */
        public final String typeArgFQName;

        /**
         * Creates a simple (non-generic) parameter.
         *
         * @param typeFQName fully-qualified type name
         * @param name variable name used in generated source
         */
        public MethodParam(String typeFQName, String name) {
            this(typeFQName, name, null);
        }

        /**
         * Creates a parameter, optionally with a type argument.
         *
         * @param typeFQName fully-qualified type name (or raw type when generic)
         * @param name variable name used in generated source
         * @param typeArgFQName if non-{@code null}, wraps {@code typeFQName} as
         *            {@code typeFQName<typeArgFQName>}
         */
        public MethodParam(String typeFQName, String name, String typeArgFQName) {
            this.typeFQName = typeFQName;
            this.name = name;
            this.typeArgFQName = typeArgFQName;
        }
    }

    private final PsiClass binding;

    /** Simple name of the method to insert (e.g. {@code "notify"}). */
    private final String methodName;

    /**
     * Fully-qualified return type name (e.g. {@code "void"}, {@code "java.lang.String"}).
     * Use {@code "void"} for methods with no return value.
     */
    private final String returnTypeFQName;

    /**
     * Visibility modifier keyword (e.g. {@code "public"}, {@code "protected"},
     * {@code "private"}). May be {@code null} or empty for package-private.
     */
    private final String modifier;

    /**
     * Ordered list of fully-qualified annotation names to prepend to the method
     * (e.g. {@code "java.lang.Override"}). May be {@code null} or empty.
     */
    private final List<String> annotationFQNames;

    /**
     * Ordered list of parameters to declare on the generated method.
     * May be {@code null} or empty for a no-arg method.
     */
    private final List<MethodParam> params;

    /**
     * Creates an {@code AddMethodProposal}.
     *
     * @param label the label shown in the quick-fix menu
     * @param sourceCU the source file (used to produce the workspace edit diff)
     * @param invocationNode the PSI file containing the class to modify
     * @param binding the {@link PsiClass} to insert the method into
     * @param relevance ordering hint
     * @param methodName simple name of the method (e.g. {@code "notify"})
     * @param returnTypeFQName fully-qualified return type (use {@code "void"} for void methods)
     * @param modifier visibility modifier keyword (e.g. {@code "public"}), or {@code null} for package-private
     * @param annotationFQNames list of fully-qualified annotation names to prepend, or {@code null}
     * @param params list of method parameters, or {@code null} for a no-arg method
     */
    public AddMethodProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                             PsiClass binding, int relevance,
                             String methodName, String returnTypeFQName, String modifier,
                             List<String> annotationFQNames, List<MethodParam> params) {
        super(label, CodeActionKind.QuickFix, binding, relevance, sourceCU);
        this.binding = binding;
        this.methodName = methodName;
        this.returnTypeFQName = returnTypeFQName;
        this.modifier = modifier;
        this.annotationFQNames = annotationFQNames;
        this.params = params;
    }

    /**
     * Builds and inserts the new method into {@link #binding} using the PSI element
     * factory, then reformats the containing file.
     *
     * <p>The generated method has the form:
     * <pre>
     *   &#64;Annotation1 &#64;Annotation2     // if annotations are specified
     *   [modifier] [returnType] &lt;methodName&gt;([paramType paramName], ...) {}
     * </pre>
     *
     * Required imports are added automatically via the PSI element factory.
     */
    @Override
    public void performUpdate() {
        PsiElementFactory factory = PsiElementFactory.getInstance(binding.getProject());

        StringBuilder sb = new StringBuilder();

        // Annotations.
        if (annotationFQNames != null) {
            for (String annotFQName : annotationFQNames) {
                String simpleName = annotFQName.substring(annotFQName.lastIndexOf('.') + 1);
                sb.append("@").append(simpleName).append("\n");
            }
        }

        // Modifier.
        if (modifier != null && !modifier.isEmpty()) {
            sb.append(modifier).append(" ");
        }

        // Return type.
        String returnTypeText = "void".equals(returnTypeFQName) ? "void" : simpleName(returnTypeFQName);
        sb.append(returnTypeText).append(" ").append(methodName).append("(");

        // Parameters.
        if (params != null && !params.isEmpty()) {
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                MethodParam p = params.get(i);
                String paramTypeText;
                if (p.typeArgFQName != null) {
                    paramTypeText = simpleName(p.typeFQName) + "<" + simpleName(p.typeArgFQName) + ">";
                } else {
                    paramTypeText = simpleName(p.typeFQName);
                }
                sb.append(paramTypeText).append(" ").append(p.name);
            }
        }

        sb.append(") {}");

        PsiMethod newMethod = factory.createMethodFromText(sb.toString(), binding);

        // Insert at the end of the class body (after existing members).
        binding.add(newMethod);
        PositionUtils.formatDocument(binding);
    }

    /**
     * Returns the simple (unqualified) name for a fully-qualified class name.
     *
     * @param fqn a fully-qualified class name, e.g. {@code "jakarta.enterprise.inject.spi.EventContext"}
     * @return the portion after the last {@code '.'}, or {@code fqn} itself if no dot is present
     */
    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(dot + 1) : fqn;
    }
}
