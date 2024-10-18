/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Himanshu Chotwani
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di;

public class DependencyInjectionConstants {

    /* Annotation Constants */
    public static final String INJECT_FQ_NAME = "jakarta.inject.Inject";

    /* Diagnostics fields constants */
    public static final String DIAGNOSTIC_SOURCE = "jakarta-di";
    public static final String DIAGNOSTIC_CODE_INJECT_FINAL = "RemoveInjectOrFinal";
    public static final String DIAGNOSTIC_CODE_INJECT_CONSTRUCTOR = "RemoveInject";

    public static final String DIAGNOSTIC_CODE_INJECT_ABSTRACT = "RemoveInjectOrAbstract";
    public static final String DIAGNOSTIC_CODE_INJECT_STATIC = "RemoveInjectOrStatic";
    public static final String DIAGNOSTIC_CODE_INJECT_GENERIC = "RemoveInjectForGeneric";
}
