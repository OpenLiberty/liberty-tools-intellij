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

/**
 * Constants for Jakarta Enterprise Beans (EJB) diagnostics.
 */
public class EjbConstants {
    /* Annotations */
    public static final String MESSAGE_DRIVEN_FQ_NAME = "jakarta.ejb.MessageDriven";
    public static final String STATELESS_FQ_NAME = "jakarta.ejb.Stateless";
    public static final String STATEFUL_FQ_NAME = "jakarta.ejb.Stateful";
    public static final String SINGLETON_FQ_NAME = "jakarta.ejb.Singleton";

    /* Interfaces */
    public static final String MESSAGE_LISTENER_FQ_NAME = "jakarta.jms.MessageListener";
    public static final String[] SESSION_BEAN_ANNOTATIONS = {
            STATELESS_FQ_NAME,
            STATEFUL_FQ_NAME,
            SINGLETON_FQ_NAME
    };

    /* Diagnostic codes */
    public static final String DIAGNOSTIC_SOURCE = "jakarta-ejb";
    public static final String DIAGNOSTIC_CODE_MISSING_MESSAGELISTENER = "ImplementMessageListener";
    public static final String DIAGNOSTIC_CODE_MISSING_PUBLIC_CONSTRUCTOR = "MissingPublicNoArgConstructor";
    public static final String DIAGNOSTIC_CODE_CONFLICTING_ANNOTATIONS = "ConflictingSessionBeanAnnotations";
    public static final String DIAGNOSTIC_CODE_FINALIZE_METHOD = "SessionBeanFinalizeMethod";

    public static final String FINALIZE_METHOD_NAME = "finalize";
}
