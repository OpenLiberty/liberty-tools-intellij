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
 *     Yijia Jing
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonp;

public class JsonpConstants {

    /* Source */
    public static final String DIAGNOSTIC_SOURCE = "jakarta-jsonp";

    /* Constants */
    public static final String CREATE_POINTER = "createPointer";
    public static final String JSON_FQ_NAME = "jakarta.json.Json";
    public static final String DIAGNOSTIC_CODE_CREATE_POINTER = "InvalidCreatePointerArg";
    public static final String DIAGNOSTIC_CODE_INVALID_OBJECT_BUILDER_KEY = "InvalidJsonObjectBuilderKey";
    public static final String DIAGNOSTIC_CODE_INVALID_ARRAY_BUILDER_VALUE = "InvalidJsonArrayBuilderValue";
    public static final String JAKARTA_JSON_OBJECT_BUILDER_FQ_NAME = "jakarta.json.JsonObjectBuilder";
    public static final String JAKARTA_JSON_BUILDER_ADD_METHOD = "add";
    public static final String JAKARTA_JSON_ARRAY_BUILDER_FQ_NAME = "jakarta.json.JsonArrayBuilder";
    public static final int EXPRESSION_COUNT_CREATE_POINTER = 1;

    /* JSON-B recommendation constants */
    public static final String CREATE_READER = "createReader";
    public static final String READ_OBJECT = "readObject";
    public static final String JSON_READER_FQ_NAME = "jakarta.json.JsonReader";
    public static final String JSON_OBJECT_FQ_NAME = "jakarta.json.JsonObject";
    public static final String GET_STRING = "getString";
    public static final String GET_INT = "getInt";
    public static final String GET_BOOLEAN = "getBoolean";
    public static final String GET_JSON_NUMBER = "getJsonNumber";
    public static final String GET_JSON_OBJECT = "getJsonObject";
    public static final String GET_JSON_ARRAY = "getJsonArray";
    public static final String DIAGNOSTIC_CODE_USE_JSONB = "UseJsonbInsteadOfManualParsing";
}
