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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PersistenceConstants {
    /* Annotation Constants */
    public static final String OBJECT = "java.lang.Object";
    public static final String ENTITY = "jakarta.persistence.Entity";
    public static final String EMBEDDED = "jakarta.persistence.Embedded";
    public static final String EMBEDDABLE = "jakarta.persistence.Embeddable";
    public static final String INHERITANCE = "jakarta.persistence.Inheritance";
    public static final String ID = "jakarta.persistence.Id";
    public static final String IDCLASS = "jakarta.persistence.IdClass";
    public static final String EMBEDDEDID = "jakarta.persistence.EmbeddedId";
    public static final String MAPPEDSUPERCLASS = "jakarta.persistence.MappedSuperclass";
    public static final String ELEMENT_COLLECTION = "jakarta.persistence.ElementCollection";
    public static final String ATTRIBUTE_OVERRIDE = "jakarta.persistence.AttributeOverride";
    public static final String ATTRIBUTE_OVERRIDES = "jakarta.persistence.AttributeOverrides";
    public static final String ASSOCIATION_OVERRIDE = "jakarta.persistence.AssociationOverride";
    public static final String ASSOCIATION_OVERRIDES = "jakarta.persistence.AssociationOverrides";

    /* @ElementCollection map prefix constants */
    public static final String MAP_KEY_PREFIX = "key.";
    public static final String MAP_VALUE_PREFIX = "value.";

    public static final String MAPKEY = "jakarta.persistence.MapKey";
    public static final String MAPKEYCLASS = "jakarta.persistence.MapKeyClass";
    public static final String MAPKEYJOINCOLUMN = "jakarta.persistence.MapKeyJoinColumn";
    public static final String MAPKEYENUMERATED = "jakarta.persistence.MapKeyEnumerated";
    public static final String MAPKEYTEMPORAL = "jakarta.persistence.MapKeyTemporal";
    public static final String CONVERT = "jakarta.persistence.Convert";
    public static final String ONE_TO_ONE = "jakarta.persistence.OneToOne";
    public static final String ONE_TO_MANY = "jakarta.persistence.OneToMany";
    public static final String MANY_TO_ONE = "jakarta.persistence.ManyToOne";
    public static final String MANY_TO_MANY = "jakarta.persistence.ManyToMany";
    public static final String ENUMERATED = "jakarta.persistence.Enumerated";
    public static final String NAMEDENTITYGRAPH = "jakarta.persistence.NamedEntityGraph";
    public static final String NAMEDENTITYGRAPHS = "jakarta.persistence.NamedEntityGraphs";
    public static final String NAMEDQUERY = "jakarta.persistence.NamedQuery";
    public static final String NAMEDQUERIES = "jakarta.persistence.NamedQueries";
    public static final String NAMEDNATIVEQUERY = "jakarta.persistence.NamedNativeQuery";
    public static final String NAMEDNATIVEQUERIES = "jakarta.persistence.NamedNativeQueries";
    public static final String TEMPORAL = "jakarta.persistence.Temporal";
    public static final String CONVERTER = "jakarta.persistence.Converter";
    public static final String ATTRIBUTE_CONVERTER = "jakarta.persistence.AttributeConverter";
    public static final String VERSION = "jakarta.persistence.Version";
    public static final String TEMPORAL_TYPE = "jakarta.persistence.TemporalType";
    public static final String TABLE_GENERATOR = "jakarta.persistence.TableGenerator";
    public static final String TABLE_GENERATORS = "jakarta.persistence.TableGenerators";
    public static final String SEQUENCE_GENERATOR = "jakarta.persistence.SequenceGenerator";
    public static final String SEQUENCE_GENERATORS = "jakarta.persistence.SequenceGenerators";
    public static final String SECONDARY_TABLE = "jakarta.persistence.SecondaryTable";
    public static final String SECONDARY_TABLES = "jakarta.persistence.SecondaryTables";
    public static final String MAPSID = "jakarta.persistence.MapsId";

    public static final String[] GENERATOR_AND_TABLE_ANNOTATIONS = {
            TABLE_GENERATOR, TABLE_GENERATORS, SEQUENCE_GENERATOR, SEQUENCE_GENERATORS, SECONDARY_TABLE, SECONDARY_TABLES
    };
    public static final String VALUE = "value";

    /* Bidirectional relationship annotation constants */
    public static final String JOIN_TABLE = "jakarta.persistence.JoinTable";
    public static final String JOIN_COLUMN = "jakarta.persistence.JoinColumn";
    public static final String JOIN_COLUMNS = "jakarta.persistence.JoinColumns";
    public static final String DIAGNOSTIC_CODE_INVERSE_MISSING_MAPPED_BY = "InverseSideMissingMappedBy";
    public static final String DIAGNOSTIC_CODE_JOIN_TABLE_ON_INVERSE = "JoinTableOnInverseSide";
    public static final String DIAGNOSTIC_CODE_JOIN_COLUMN_ON_INVERSE = "JoinColumnOnInverseSide";

    /** All relationship annotations that support the {@code mappedBy} attribute. */
    public static final String[] INVERSE_CAPABLE_RELATIONSHIP_ANNOTATIONS = {
            ONE_TO_MANY, ONE_TO_ONE, MANY_TO_MANY
    };

    /**
     * Annotations that are only valid on the owning side of a relationship
     * and must not appear on the inverse side (i.e. the side that carries
     * {@code mappedBy}).
     */
    public static final String[] OWNER_ONLY_ANNOTATIONS = {
            JOIN_TABLE, JOIN_COLUMN, JOIN_COLUMNS
    };

    /** Annotation attribute name for the owning-side field reference. */
    public static final String MAPPED_BY = "mappedBy";

    /* Type Constants */
    public static final String MAP_INTERFACE_FQDN = "java.util.Map";

    /* Annotation Fields */
    public static final String NAME = "name";
    public static final String REFERENCEDCOLUMNNAME = "referencedColumnName";
    public static final String CONVERTER_ATTRIBUTE = "converter";
    public static final String DISABLE_CONVERSION = "disableConversion";

    /* @Convert restricted-target annotations */
    public static final String[] CONVERT_RESTRICTED_ANNOTATIONS = {
        ID, VERSION, ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY, ENUMERATED, TEMPORAL
    };

    /* String search constants */
    public static final int NOT_FOUND = -1;

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
    public static final String DIAGNOSTIC_CODE_EMBEDDED_NOT_EMBEDDABLE = "EmbeddedTypeNotAnnotatedWithEmbeddable";
    public static final String DIAGNOSTIC_CODE_INVALID_ID_TYPE = "InvalidIdType";
    public static final String DIAGNOSTIC_CODE_MULTIPLE_EMBEDDED_ID = "MultipleEmbeddedIdAnnotations";
    public static final String DIAGNOSTIC_CODE_MIXED_IDENTIFIER = "MixedIdentifierAnnotations";
    public static final String DIAGNOSTIC_CODE_IDCLASS_MEMBER_MISSING_IN_KEY_CLASS = "IdClassMemberMissingInKeyClass";
    public static final String DIAGNOSTIC_CODE_IDCLASS_MEMBER_TYPE_MISMATCH = "IdClassMemberTypeMismatch";
    public static final String DIAGNOSTIC_CODE_NAMED_ENTITY_GRAPH_ON_NON_ENTITY = "NamedEntityGraphOnNonEntityClass";
    public static final String DIAGNOSTIC_CODE_NAMED_ENTITY_GRAPHS_ON_NON_ENTITY = "NamedEntityGraphsOnNonEntityClass";
    public static final String DIAGNOSTIC_CODE_NAMED_QUERY_ON_INVALID_CLASS = "NamedQueryOnInvalidClass";
    public static final String DIAGNOSTIC_CODE_NAMED_QUERIES_ON_INVALID_CLASS = "NamedQueriesOnInvalidClass";
    public static final String DIAGNOSTIC_CODE_NAMED_NATIVE_QUERY_ON_INVALID_CLASS = "NamedNativeQueryOnInvalidClass";
    public static final String DIAGNOSTIC_CODE_NAMED_NATIVE_QUERIES_ON_INVALID_CLASS = "NamedNativeQueriesOnInvalidClass";
    public static final String DIAGNOSTIC_CODE_INHERITANCE_ON_NON_ENTITY = "InheritanceAnnotationOnNonEntityClass";
    public static final String DIAGNOSTIC_CODE_INHERITANCE_ON_NON_ROOT = "InheritanceAnnotationOnNonRootEntity";
    
    /* Generator / SecondaryTable Codes */
    public static final String DIAGNOSTIC_CODE_TABLE_GENERATOR_INVALID_EMPTY_NAME = "TableGeneratorInvalidEmptyName";
    public static final String DIAGNOSTIC_CODE_TABLE_GENERATORS_MISSING_MAPPING = "TableGeneratorsMissingTableGeneratorMapping";
    public static final String DIAGNOSTIC_CODE_SEQUENCE_GENERATOR_INVALID_EMPTY_NAME = "SequenceGeneratorInvalidEmptyName";
    public static final String DIAGNOSTIC_CODE_SEQUENCE_GENERATORS_MISSING_MAPPING = "SequenceGeneratorsMissingSequenceGeneratorMapping";
    public static final String DIAGNOSTIC_CODE_SECONDARY_TABLE_INVALID_EMPTY_NAME = "SecondaryTableInvalidEmptyName";
    public static final String DIAGNOSTIC_CODE_SECONDARY_TABLES_MISSING_MAPPING = "SecondaryTablesMissingSecondaryTableMapping";

    /* Converter Codes */
    public static final String DIAGNOSTIC_CODE_CONVERTER_MUST_IMPLEMENT = "ConverterMustImplementAttributeConverter";

    /* MapKey Codes */
    public static final String DIAGNOSTIC_CODE_MAPKEYENUMERATED_NON_MAP = "MapKeyEnumeratedOnNonMapType";
    public static final String DIAGNOSTIC_CODE_MAPKEYENUMERATED_NON_ENUM = "MapKeyEnumeratedOnNonEnumType";
    public static final String DIAGNOSTIC_CODE_INVALID_ANNOTATION = "RemoveMapKeyorMapKeyClass";
    public static final String DIAGNOSTIC_CODE_MISSING_ATTRIBUTES = "SupplyAttributesToAnnotations";
    public static final String DIAGNOSTIC_CODE_INVALID_ACCESS_SPECIFIER = "InvalidMethodAccessSpecifier";
    public static final String DIAGNOSTIC_CODE_INVALID_METHOD_NAME = "InvalidMethodName";
    public static final String DIAGNOSTIC_CODE_FIELD_NOT_EXIST = "InvalidMapKeyAnnotationsFieldNotFound";
    public static final String DIAGNOSTIC_CODE_INVALID_RETURN_TYPE = "InvalidReturnTypeOfMethod";
    public static final String DIAGNOSTIC_CODE_INVALID_TYPE = "InvalidTypeOfField";
    public static final String DIAGNOSTIC_CODE_INVALID_MAPKEYTEMPORAL_TYPE = "MapKeyTemporalNotOnTemporalType";

    /* AttributeOverride / AssociationOverride Codes */
    public static final String DIAGNOSTIC_CODE_INVALID_ATTRIBUTE_OVERRIDE_NAME = "InvalidAttributeOverrideName";
    public static final String DIAGNOSTIC_CODE_INVALID_ASSOCIATION_OVERRIDE_NAME = "InvalidAssociationOverrideName";

    /* PersistenceContext Codes */
    public static final String PERSISTENCE_CONTEXT = "jakarta.persistence.PersistenceContext";
    public static final String PERSISTENCE_CONTEXT_TYPE_EXTENDED = "PersistenceContextType.EXTENDED";
    public static final String DIAGNOSTIC_CODE_PERSISTENCE_CONTEXT_NOT_IN_MANAGED_COMPONENT = "PersistenceContextNotInManagedComponent";
    public static final String DIAGNOSTIC_CODE_EXTENDED_CONTEXT_IN_NON_STATEFUL = "ExtendedPersistenceContextInNonStatefulBean";

    /* @Convert Codes */
    public static final String DIAGNOSTIC_CODE_CONVERT_MISSING_CONVERTER_OR_DISABLE = "InvalidConvertAnnotationMissingConverterOrDisable";
    public static final String DIAGNOSTIC_CODE_CONVERT_ON_RESTRICTED_TARGET = "InvalidConvertAnnotationOnRestrictedTarget";
    public static final String DIAGNOSTIC_CODE_CONVERT_MULTIPLE_ON_SAME_ATTRIBUTE = "MultipleConvertAnnotationOnSameAttribute";
    /* primitive → wrapper FQ name mapping for derived-identity type checks (spec §2.4) */
    public static final Map<String, String> PRIMITIVE_TO_WRAPPER = Map.of(
            "int",     "java.lang.Integer",
            "long",    "java.lang.Long",
            "short",   "java.lang.Short",
            "byte",    "java.lang.Byte",
            "char",    "java.lang.Character",
            "boolean", "java.lang.Boolean",
            "float",   "java.lang.Float",
            "double",  "java.lang.Double");

    /* Derived Identity error codes */
    public static final String DIAGNOSTIC_CODE_MAPSID_ON_INVALID_RELATIONSHIP = "MapsIdOnInvalidRelationship";
    public static final String DIAGNOSTIC_CODE_MAPSID_REQUIRES_COMPOSITE_KEY   = "MapsIdRequiresCompositeKey";
    public static final String DIAGNOSTIC_CODE_MAPSID_VALUE_NOT_IN_EMBEDDEDID  = "MapsIdValueNotInEmbeddedIdClass";
    public static final String DIAGNOSTIC_CODE_MAPSID_VALUE_TYPE_MISMATCH      = "MapsIdValueTypeMismatch";
    public static final String DIAGNOSTIC_CODE_MAPSID_PK_TYPE_MISMATCH         = "MapsIdPrimaryKeyTypeMismatch";

    public final static String[] SET_OF_PERSISTENCE_ANNOTATIONS = {MAPKEY, MAPKEYCLASS, MAPKEYJOINCOLUMN, MAPKEYENUMERATED};
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

    public static final String HTTP_SERVLET_FQ_NAME = "jakarta.servlet.http.HttpServlet";
}
