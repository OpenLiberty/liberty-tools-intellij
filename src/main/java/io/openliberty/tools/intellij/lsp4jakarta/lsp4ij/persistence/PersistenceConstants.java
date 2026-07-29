/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation, Ankush Sharma and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Ankush Sharma - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PersistenceConstants {
    /* Annotation Constants */
    public static final String OBJECT = "java.lang.Object";
    public static final String ENTITY = "jakarta.persistence.Entity";
    public static final String ID = "jakarta.persistence.Id";
    public static final String EMBEDDEDID = "jakarta.persistence.EmbeddedId";
    public static final String MAPPEDSUPERCLASS = "jakarta.persistence.MappedSuperclass";
    public static final String MAPKEY = "jakarta.persistence.MapKey";
    public static final String MAPKEYCLASS = "jakarta.persistence.MapKeyClass";
    public static final String MAPKEYJOINCOLUMN = "jakarta.persistence.MapKeyJoinColumn";
    public static final String MAPKEYTEMPORAL = "jakarta.persistence.MapKeyTemporal";
    public static final String TEMPORAL = "jakarta.persistence.Temporal";
    public static final String VERSION = "jakarta.persistence.Version";
    public static final String TEMPORAL_TYPE = "jakarta.persistence.TemporalType";

    /* Annotation Fields */
    public static final String NAME = "name";
    public static final String REFERENCEDCOLUMNNAME = "referencedColumnName";

    /* Source */
    public static final String DIAGNOSTIC_SOURCE = "jakarta-persistence";

    /* Entity Codes */
    public static final String DIAGNOSTIC_CODE_MISSING_EMPTY_CONSTRUCTOR = "MissingEmptyConstructor";
    public static final String DIAGNOSTIC_CODE_FINAL_METHODS = "RemoveFinalMethods";
    public static final String DIAGNOSTIC_CODE_FINAL_VARIABLES = "RemoveFinalVariables";
    public static final String DIAGNOSTIC_CODE_FINAL_CLASS = "InvalidClass";
    public static final String DIAGNOSTIC_CODE_MISSING_PRIMARY_KEY = "MissingPrimaryKey";
    public static final String DIAGNOSTIC_CODE_TEMPORAL_INVALID_VALUE = "InvalidValueInTemporalAnnotation";
    public static final String DIAGNOSTIC_CODE_MISSING_TEMPORAL = "MissingTemporalAnnotation";
    public static final String DIAGNOSTIC_CODE_DUPLICATE_VERSION = "MultipleVersionAnnotations";
    public static final String DIAGNOSTIC_CODE_VERSION_IN_HIERARCHY = "VersionAnnotationInHierarchy";
    public static final String DIAGNOSTIC_CODE_INVALID_VERSION_TYPE = "InvalidVersionFieldOrPropertyType";
    public static final String DIAGNOSTIC_CODE_INVALID_ID_TYPE = "InvalidIdType";


    /* MapKey Codes */
    public static final String DIAGNOSTIC_CODE_INVALID_ANNOTATION = "RemoveMapKeyorMapKeyClass";
    public static final String DIAGNOSTIC_CODE_MISSING_ATTRIBUTES = "SupplyAttributesToAnnotations";
    public static final String DIAGNOSTIC_CODE_INVALID_ACCESS_SPECIFIER = "InvalidMethodAccessSpecifier";
    public static final String DIAGNOSTIC_CODE_INVALID_METHOD_NAME = "InvalidMethodName";
    public static final String DIAGNOSTIC_CODE_FIELD_NOT_EXIST = "InvalidMapKeyAnnotationsFieldNotFound";
    public static final String DIAGNOSTIC_CODE_INVALID_RETURN_TYPE = "InvalidReturnTypeOfMethod";
    public static final String DIAGNOSTIC_CODE_INVALID_TYPE = "InvalidTypeOfField";
    public static final String DIAGNOSTIC_CODE_INVALID_MAPKEYTEMPORAL_TYPE = "MapKeyTemporalNotOnTemporalType";

    public final static String[] SET_OF_PERSISTENCE_ANNOTATIONS = {MAPKEY, MAPKEYCLASS, MAPKEYJOINCOLUMN};
    public static final String[] SET_OF_PRIMARY_KEY_DATE_ANNOTATIONS = { ID, TEMPORAL };
    public static final Set<String> SET_OF_VALID_VERSION_TYPES = Set.of(
            "int", "short", "long", "java.lang.Integer",
            "java.lang.Short", "java.lang.Long", "java.sql.Timestamp");
    
    /* Valid @Id type sets */
    private static final Set<String> VALID_ID_PRIMITIVES = Set.of(
            "int", "long", "short", "byte", "char", "boolean", "float", "double");
    private static final Set<String> VALID_ID_WRAPPERS = Set.of(
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
            "java.lang.Long", "java.lang.Character","java.lang.Boolean", "java.lang.Float", "java.lang.Double");
    private static final Set<String> VALID_ID_DATES = Set.of(
            "java.util.Date", "java.sql.Date");
    private static final Set<String> VALID_ID_BIG_NUMBERS = Set.of(
            "java.math.BigDecimal", "java.math.BigInteger");
    private static final Set<String> VALID_ID_STRING = Set.of("java.lang.String");
    
    // Combined set for easy validation using streams
    public static final Set<String> SET_OF_VALID_ID_TYPES = Stream.of(
            VALID_ID_PRIMITIVES, VALID_ID_WRAPPERS, VALID_ID_DATES,
            VALID_ID_BIG_NUMBERS, VALID_ID_STRING)
            .flatMap(Set::stream)
            .collect(Collectors.toUnmodifiableSet());
    
    public static final String UTIL_DATE = "java.util.Date";
    public static final String UTIL_CALENDAR = "java.util.Calendar";
    public static final String TEMPORAL_TYPE_DATE = "TemporalType.DATE";
}
