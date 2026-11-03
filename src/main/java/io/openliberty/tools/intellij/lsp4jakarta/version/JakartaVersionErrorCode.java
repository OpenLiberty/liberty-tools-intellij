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

package io.openliberty.tools.intellij.lsp4jakarta.version;

import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaErrorCode;

/**
 * Jakarta version diagnostic codes.
 */
public enum JakartaVersionErrorCode implements IJavaErrorCode {
    MultipleJakartaEEVersions;

    @Override
    public String getCode() {
        return name();
    }
}
