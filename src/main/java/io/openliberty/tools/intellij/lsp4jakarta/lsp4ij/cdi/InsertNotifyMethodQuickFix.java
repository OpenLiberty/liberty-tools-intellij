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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.AddMethodProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.AddMethodProposal.MethodParam;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base quick fix that inserts a required {@code notify} override into a class
 * that implements {@code ObserverMethod} without providing the method.
 *
 * <p>Two concrete subclasses cover the two overloads:
 * <ul>
 * <li>{@link NotifyEvent} — inserts {@code notify(T event)}</li>
 * <li>{@link NotifyEventContext} — inserts {@code notify(EventContext<T> eventContext)}</li>
 * </ul>
 *
 * <p>The concrete type argument {@code T} is resolved from
 * {@code ObserverMethod<T>} at quick-fix time (falls back to {@code Object}).
 */
abstract class InsertNotifyMethodQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(InsertNotifyMethodQuickFix.class.getName());

    /** Which of the two notify overloads this instance inserts. */
    private final boolean isEventContext;

    /** Message-key used to look up the label in {@code messages.properties}. */
    private final String labelKey;

    /**
     * Constructs a quick fix for one of the two {@code notify} overloads.
     *
     * @param isEventContext {@code true} to insert the {@code notify(EventContext<T>)} overload;
     *                       {@code false} to insert the {@code notify(T)} overload
     * @param labelKey       resource-bundle key used to look up the quick-fix label
     */
    protected InsertNotifyMethodQuickFix(boolean isEventContext, String labelKey) {
        this.isEventContext = isEventContext;
        this.labelKey = labelKey;
    }

    /**
     * Returns a single code action whose title is derived from the resolved type argument
     * of {@code ObserverMethod<T>} on the enclosing class.
     *
     * @param context    the code action context
     * @param diagnostic the diagnostic that triggered this quick fix
     * @return a singleton list containing the unresolved code action, or an empty list
     *         if the enclosing class cannot be determined
     */
    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        if (parentType == null) {
            return Collections.emptyList();
        }
        String typeArgSimple = resolveTypeArgSimpleName(parentType);
        String label = Messages.getMessage(labelKey, typeArgSimple);
        return Collections.singletonList(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
    }

    /**
     * Resolves the code action by constructing an {@link AddMethodProposal} that inserts
     * the appropriate {@code notify} override into the target class and converts it to a
     * workspace edit.
     *
     * @param context the resolve context carrying the unresolved code action and AST root
     * @return the resolved code action with its {@code edit} populated, or the original
     *         unresolved action if the enclosing class cannot be determined
     */
    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        if (parentType == null) {
            return toResolve;
        }

        String typeArgFQName = resolveTypeArgFQName(parentType);
        String typeArgSimple = simpleName(typeArgFQName);
        String label = Messages.getMessage(labelKey, typeArgSimple);

        MethodParam param;
        if (!isEventContext) {
            // notify(AuditEvent event)
            param = new MethodParam(typeArgFQName, "event");
        } else {
            // notify(EventContext<AuditEvent> eventContext)
            param = new MethodParam(ManagedBeanConstants.EVENT_CONTEXT_FQ_NAME, "eventContext", typeArgFQName);
        }

        ChangeCorrectionProposal proposal = new AddMethodProposal(
                label,
                context.getSource().getCompilationUnit(),
                context.getASTRoot(),
                parentType,
                0,
                "notify",
                "void",
                "public",
                Collections.singletonList("java.lang.Override"),
                Collections.singletonList(param));

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                "Unable to create workspace edit for code action to insert notify method override");
        return toResolve;
    }

    /**
     * Returns the fully-qualified name of the type argument {@code T} from
     * {@code ObserverMethod<T>} declared on the given class.
     *
     * @param classBinding the class whose {@code implements} list is inspected
     * @return the FQN of the first type argument of {@code ObserverMethod<T>},
     *         or {@code "java.lang.Object"} if it cannot be resolved
     */
    private static String resolveTypeArgFQName(PsiClass classBinding) {
        for (PsiClassType ifaceType : classBinding.getImplementsListTypes()) {
            PsiClass iface = ifaceType.resolve();
            if (iface != null && ManagedBeanConstants.OBSERVER_METHOD_FQ_NAME.equals(iface.getQualifiedName())) {
                PsiType[] typeArgs = ifaceType.getParameters();
                if (typeArgs.length > 0) {
                    return typeArgs[0].getCanonicalText();
                }
            }
        }
        return "java.lang.Object";
    }

    /**
     * Returns the simple (unqualified) name of the type argument {@code T} from
     * {@code ObserverMethod<T>} declared on the given class.
     *
     * @param classBinding the class whose {@code implements} list is inspected
     * @return the simple name of the type argument, e.g. {@code "AuditEvent"},
     *         or {@code "Object"} if it cannot be resolved
     */
    private static String resolveTypeArgSimpleName(PsiClass classBinding) {
        return simpleName(resolveTypeArgFQName(classBinding));
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

    // -------------------------------------------------------------------------
    // Concrete subclasses
    // -------------------------------------------------------------------------

    /**
     * Quick fix that inserts {@code @Override public void notify(T event) {}}
     * using the concrete type argument resolved from {@code ObserverMethod<T>}.
     */
    public static class NotifyEvent extends InsertNotifyMethodQuickFix {

        /**
         * Creates the {@code notify(T event)} quick fix.
         */
        public NotifyEvent() {
            super(false, "InsertNotifyEventMethod");
        }

        /**
         * {@inheritDoc}
         *
         * @return the fully-qualified class name of {@link NotifyEvent}
         */
        @Override
        public String getParticipantId() {
            return NotifyEvent.class.getName();
        }
    }

    /**
     * Quick fix that inserts {@code @Override public void notify(EventContext<T> eventContext) {}}
     * using the concrete type argument resolved from {@code ObserverMethod<T>}.
     */
    public static class NotifyEventContext extends InsertNotifyMethodQuickFix {

        /**
         * Creates the {@code notify(EventContext&lt;T&gt; eventContext)} quick fix.
         */
        public NotifyEventContext() {
            super(true, "InsertNotifyEventContextMethod");
        }

        /**
         * {@inheritDoc}
         *
         * @return the fully-qualified class name of {@link NotifyEventContext}
         */
        @Override
        public String getParticipantId() {
            return NotifyEventContext.class.getName();
        }
    }
}
