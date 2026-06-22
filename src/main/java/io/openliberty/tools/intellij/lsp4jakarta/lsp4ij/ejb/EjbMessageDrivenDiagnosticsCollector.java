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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.Constants.DIAGNOSTIC_CODE;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.Constants.DIAGNOSTIC_SOURCE;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.Constants.MESSAGE_DRIVEN_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.Constants.MESSAGE_LISTENER_FQ_NAME;

import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

/**
 * Jakarta Enterprise Beans (EJB) @MessageDriven diagnostics collector.
 * 
 * Validates that classes annotated with @MessageDriven implement the appropriate
 * message listener interface. For JMS message-driven beans (the default case),
 * this requires implementing jakarta.jms.MessageListener.
 * 
 * Diagnostic:
 * - A class annotated with @MessageDriven must implement a message listener interface.
 *   For JMS message-driven beans, implement jakarta.jms.MessageListener.
 * 
 * @see <a href="https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#the-required-message-listener-interface">
 *      Jakarta EE Enterprise Beans Specification - Section 5.4.2</a>
 */
public class EjbMessageDrivenDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public EjbMessageDrivenDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }

        PsiClass[] allTypes = unit.getClasses();
        
        for (PsiClass type : allTypes) {
            // Check if class has @MessageDriven annotation
            PsiAnnotation messageDrivenAnnotation = type.getAnnotation(MESSAGE_DRIVEN_FQ_NAME);
            
            if (messageDrivenAnnotation != null) {
                // Check if the class implements MessageListener interface
                if (!implementsMessageListener(type)) {
                    // Create diagnostic on the class name
                    diagnostics.add(createDiagnostic(
                        type,
                        unit,
                        Messages.getMessage("MessageDrivenMustImplementMessageListener"),
                        DIAGNOSTIC_CODE,
                        null,
                        DiagnosticSeverity.Error
                    ));
                }
            }
        }
    }

    /**
     * Checks if the given class implements jakarta.jms.MessageListener interface.
     * 
     * @param type the class to check
     * @return true if the class implements MessageListener, false otherwise
     */
    private boolean implementsMessageListener(PsiClass type) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(type.getProject());
        PsiClass messageListenerClass = facade.findClass(
            MESSAGE_LISTENER_FQ_NAME,
            GlobalSearchScope.allScope(type.getProject())
        );
        
        // If MessageListener interface is not found in the project, we can't validate
        if (messageListenerClass == null) {
            return true; // Assume valid to avoid false positives
        }
        
        // Check if the class implements MessageListener (directly or through inheritance)
        return type.isInheritor(messageListenerClass, true);
    }
}