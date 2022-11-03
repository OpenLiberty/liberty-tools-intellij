/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.liberty.lsp;

import com.intellij.lang.Language;

/**
 * Custom language for bootstrap.properties files
 */
public class BootstrapPropertiesLanguage extends Language {

    public static final BootstrapPropertiesLanguage INSTANCE = new BootstrapPropertiesLanguage();

    protected BootstrapPropertiesLanguage() {
        super("BootstrapProperties", "text/properties");
    }
}
