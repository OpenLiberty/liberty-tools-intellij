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

/**
 * Code action proposal for adding a public void method with a single parameter to a class.
 *
 * <p>Supports inserting an optional {@code @Override} annotation and an optional
 * parameterized type argument (e.g. {@code EventContext<AuditEvent>}).
 *
 * <p>Used to insert required {@code notify} overrides on custom
 * {@code ObserverMethod} implementations.
 */
public class AddMethodProposal extends ASTRewriteCorrectionProposal {

    private final PsiFile sourceCU;
    private final PsiClass binding;

    /** Simple name of the method to insert (e.g. {@code "notify"}). */
    private final String methodName;

    /**
     * Fully-qualified type name of the single method parameter.
     * For a parameterized type this is the raw type FQN (e.g. {@code "jakarta.enterprise.inject.spi.EventContext"}).
     */
    private final String paramTypeFQName;

    /** Variable name used for the parameter in generated source (e.g. {@code "event"}). */
    private final String paramName;

    /**
     * Optional FQN of the type argument when the parameter is generic,
     * e.g. {@code "io.openliberty.sample.jakarta.cdi.AuditEvent"} for
     * {@code EventContext<AuditEvent>}. {@code null} means simple type.
     */
    private final String typeArgFQName;

    /** Whether to prepend {@code @Override} to the method. */
    private final boolean addOverride;

    /**
     * Creates an {@code AddMethodProposal}.
     *
     * @param label          the label shown in the quick-fix menu
     * @param sourceCU       the source file (used to produce the workspace edit diff)
     * @param invocationNode the PSI file containing the class to modify
     * @param binding        the {@link PsiClass} to insert the method into
     * @param relevance      ordering hint
     * @param methodName     simple name of the method (e.g. {@code "notify"})
     * @param paramTypeFQName FQN of the parameter type (raw if generic)
     * @param paramName      parameter variable name
     * @param typeArgFQName  FQN of the type argument for a parameterized param, or {@code null}
     * @param addOverride    whether to add {@code @Override}
     */
    public AddMethodProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                             PsiClass binding, int relevance,
                             String methodName, String paramTypeFQName, String paramName,
                             String typeArgFQName, boolean addOverride) {
        super(label, CodeActionKind.QuickFix, binding, relevance, sourceCU);
        this.sourceCU = sourceCU;
        this.binding = binding;
        this.methodName = methodName;
        this.paramTypeFQName = paramTypeFQName;
        this.paramName = paramName;
        this.typeArgFQName = typeArgFQName;
        this.addOverride = addOverride;
    }

    /**
     * Builds and inserts the new method into {@link #binding} using the PSI element
     * factory, then reformats the containing file.
     *
     * <p>The generated method signature is:
     * <pre>
     *   &#64;Override                          // if addOverride is true
     *   public void &lt;methodName&gt;(&lt;paramType&gt; &lt;paramName&gt;) {}
     * </pre>
     * where {@code <paramType>} is {@code paramTypeFQName} (simple name only) for a
     * plain type, or {@code rawType<typeArg>} when {@link #typeArgFQName} is set.
     */
    @Override
    public void performUpdate() {
        PsiElementFactory factory = PsiElementFactory.getInstance(binding.getProject());

        // Build the parameter type text, e.g. "EventContext<AuditEvent>" or "AuditEvent"
        String paramTypeText;
        if (typeArgFQName != null) {
            String rawSimple = simpleName(paramTypeFQName);
            String argSimple = simpleName(typeArgFQName);
            paramTypeText = rawSimple + "<" + argSimple + ">";
        } else {
            paramTypeText = simpleName(paramTypeFQName);
        }

        // Build the method text, e.g.:
        //   @Override
        //   public void notify(AuditEvent event) {}
        StringBuilder sb = new StringBuilder();
        if (addOverride) {
            sb.append("@Override\n");
        }
        sb.append("public void ").append(methodName)
          .append("(").append(paramTypeText).append(" ").append(paramName).append(") {}");

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
