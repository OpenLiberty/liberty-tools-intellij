 /*******************************************************************************
 * Copyright (c) 2026 IBM Corporation, Matheus Cruz and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Archana Iyer - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb;
 
/**
 * Enum for categorizing method calls in Jsonb diagnostics.
 */
public enum MethodCategory {
    CLOSE,
    THREAD_OPERATION,
    SYNCHRONIZATION,
    UNKNOWN
}