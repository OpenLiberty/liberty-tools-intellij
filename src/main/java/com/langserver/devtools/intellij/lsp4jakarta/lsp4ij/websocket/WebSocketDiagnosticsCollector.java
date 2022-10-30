/******************************************************************************* 
 * Copyright (c) 2022 IBM Corporation and others. 
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v. 2.0 which is available at 
 * http://www.eclipse.org/legal/epl-2.0. 
 * 
 * SPDX-License-Identifier: EPL-2.0 
 * 
 * Contributors: 
 *     Giancarlo Pernudi Segura - initial API and implementation
 *     Lidia Ataupillco Ramos
 *     Aviral Saxena
 *******************************************************************************/

package com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.websocket;

import java.util.*;
import java.util.stream.Stream;

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

public class WebSocketDiagnosticsCollector extends AbstractDiagnosticsCollector {
    public WebSocketDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return WebSocketConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }

        PsiClass[] alltypes = null;
        HashMap<String, Boolean> checkWSEnd = null;
        alltypes = unit.getClasses();
        for (PsiClass type : alltypes) {
            checkWSEnd = isWSEndpoint(type);
            // checks if the class uses annotation to create a WebSocket endpoint
            if (checkWSEnd.get(WebSocketConstants.IS_ANNOTATION)) {
                // WebSocket Invalid Parameters Diagnostic
                invalidParamsCheck(type, unit, diagnostics);

                /* @PathParam Value Mismatch Warning */
                List<String> endpointPathVars = findAndProcessEndpointURI(type);
                /*
                 * WebSocket endpoint annotations must be attached to a class, and thus is
                 * guaranteed to be processed before any of the member method annotations
                 */
                if (endpointPathVars != null && !endpointPathVars.isEmpty()) {
                    // PathParam URI Mismatch Warning Diagnostic
                    uriMismatchWarningCheck(type, endpointPathVars, diagnostics, unit);
                }

                // OnMessage validation for WebSocket message formats
                onMessageWSMessageFormats(type, diagnostics, unit);

                // ServerEndpoint annotation diagnostics
                serverEndpointErrorCheck(type, diagnostics, unit);
            }
        }
    }

    private void invalidParamsCheck(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        PsiMethod[] allMethods = type.getMethods();
        for (PsiMethod method : allMethods) {
            PsiAnnotation[] allAnnotations = method.getAnnotations();
            Set<String> specialParamTypes = null, rawSpecialParamTypes = null;

            for (PsiAnnotation annotation : allAnnotations) {
                String annotationName = annotation.getQualifiedName();
                String diagnosticCode = null;

                if (isMatchedJavaElement(type, annotationName, WebSocketConstants.ON_OPEN)) {
                    specialParamTypes = WebSocketConstants.ON_OPEN_PARAM_OPT_TYPES;
                    rawSpecialParamTypes = WebSocketConstants.RAW_ON_OPEN_PARAM_OPT_TYPES;
                    diagnosticCode = WebSocketConstants.DIAGNOSTIC_CODE_ON_OPEN_INVALID_PARAMS;
                } else if (isMatchedJavaElement(type, annotationName, WebSocketConstants.ON_CLOSE)) {
                    specialParamTypes = WebSocketConstants.ON_CLOSE_PARAM_OPT_TYPES;
                    rawSpecialParamTypes = WebSocketConstants.RAW_ON_CLOSE_PARAM_OPT_TYPES;
                    diagnosticCode = WebSocketConstants.DIAGNOSTIC_CODE_ON_CLOSE_INVALID_PARAMS;
                }
                if (diagnosticCode != null) {
                    PsiParameter[] allParams = method.getParameterList().getParameters();
                    for (PsiParameter param : allParams) {
                        String resolvedTypeName = param.getType().getCanonicalText();
                        boolean isPrimitive = param.getType() instanceof PsiPrimitiveType;
                        boolean isSpecialType = specialParamTypes.contains(resolvedTypeName);
                        boolean isPrimWrapped = isWrapper(resolvedTypeName);

                        // check parameters valid types
                        if (!(isSpecialType || isPrimWrapped || isPrimitive)) {
                            diagnostics.add(createDiagnostic(param, unit,
                                    createParamTypeDiagMsg(specialParamTypes, annotationName), diagnosticCode, null,
                                    DiagnosticSeverity.Error));
                            continue;
                        }

                        if (!isSpecialType) {
                            // check that if parameter is not a specialType, it has a @PathParam annotation
                            PsiAnnotation[] param_annotations = param.getAnnotations();
                            boolean hasPathParamAnnot = Arrays.asList(param_annotations).stream().anyMatch(annot -> {
                                return isMatchedJavaElement(type, annot.getQualifiedName(),
                                        WebSocketConstants.PATH_PARAM_ANNOTATION);
                            });
                            if (!hasPathParamAnnot) {
                                diagnostics.add(createDiagnostic(param, unit,
                                        WebSocketConstants.DIAGNOSTIC_PATH_PARAMS_ANNOT_MISSING,
                                        WebSocketConstants.DIAGNOSTIC_CODE_PATH_PARAMS_ANNOT, null,
                                        DiagnosticSeverity.Error));
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * Creates a warning diagnostic if a PathParam annotation does not match any
     * variable parameters of the WebSocket EndPoint URI associated with the class
     * in which the method is contained
     *
     * @param type representing the class list of diagnostics for this class
     *             compilation unit with which the type is associated
     */
    private void uriMismatchWarningCheck(PsiClass type, List<String> endpointPathVars, List<Diagnostic> diagnostics,
                                         PsiJavaFile unit) {
        PsiMethod[] typeMethods = type.getMethods();
        for (PsiMethod method : typeMethods) {
            PsiParameter[] methodParams = method.getParameterList().getParameters();
            for (PsiParameter param : methodParams) {
                PsiAnnotation[] paramAnnotations = param.getAnnotations();
                for (PsiAnnotation annotation : paramAnnotations) {
                    if (isMatchedJavaElement(type, annotation.getQualifiedName(),
                            WebSocketConstants.PATHPARAM_ANNOTATION)) {
                        PsiNameValuePair[] valuePairs = annotation.getParameterList().getAttributes();
                        for (PsiNameValuePair pair : valuePairs) {
                            String annoArgName = pair.getName();
                            if (annoArgName == null || annoArgName.equals(WebSocketConstants.ANNOTATION_VALUE)) {
                                String pathValue = pair.getLiteralValue();
                                if (!endpointPathVars.contains(pathValue)) {
                                    diagnostics.add(createDiagnostic(annotation, unit,
                                            WebSocketConstants.PATHPARAM_VALUE_WARN_MSG,
                                            WebSocketConstants.PATHPARAM_DIAGNOSTIC_CODE, null,
                                            DiagnosticSeverity.Warning));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates an error diagnostic if there exists more than one method annotated
     * with @OnMessage for a given message format.
     *
     * @param type
     * @param diagnostics
     * @param unit
     */

    private void onMessageWSMessageFormats(PsiClass type, List<Diagnostic> diagnostics, PsiJavaFile unit) {
        PsiMethod[] typeMethods = type.getMethods();
        PsiAnnotation onMessageTextUsed = null;
        PsiAnnotation onMessageBinaryUsed = null;
        PsiAnnotation onMessagePongUsed = null;
        for (PsiMethod method : typeMethods) {
            PsiAnnotation[] allAnnotations = method.getAnnotations();
            for (PsiAnnotation annotation : allAnnotations) {
                if (isMatchedJavaElement(type, annotation.getQualifiedName(), WebSocketConstants.ON_MESSAGE)) {
                    PsiParameter[] allParams = method.getParameterList().getParameters();
                    for (PsiParameter param : allParams) {
                        if (!isParamPath(type, param)) {
                            String typeName = param.getType().getCanonicalText();

                            if (typeName != null
                                    && WebSocketConstants.LONG_MESSAGE_CLASSES.contains(typeName)) {
                                WebSocketConstants.MESSAGE_FORMAT messageFormat = typeName != null
                                        ? getMessageFormat(typeName, true)
                                        : getMessageFormat(typeName, false);
                                switch (messageFormat) {
                                    case TEXT:
                                        if (onMessageTextUsed != null) {
                                            diagnostics.add(createDiagnostic(annotation, unit,
                                                    WebSocketConstants.DIAGNOSTIC_ON_MESSAGE_DUPLICATE_METHOD,
                                                    WebSocketConstants.DIAGNOSTIC_CODE_ON_MESSAGE_DUPLICATE_METHOD, null,
                                                    DiagnosticSeverity.Error));
                                            diagnostics.add(createDiagnostic(onMessageTextUsed, unit,
                                                    WebSocketConstants.DIAGNOSTIC_ON_MESSAGE_DUPLICATE_METHOD,
                                                    WebSocketConstants.DIAGNOSTIC_CODE_ON_MESSAGE_DUPLICATE_METHOD, null,
                                                    DiagnosticSeverity.Error));
                                        }
                                        onMessageTextUsed = annotation;
                                        break;
                                    case BINARY:
                                        if (onMessageBinaryUsed != null) {
                                            diagnostics.add(createDiagnostic(annotation, unit,
                                                    WebSocketConstants.DIAGNOSTIC_ON_MESSAGE_DUPLICATE_METHOD,
                                                    WebSocketConstants.DIAGNOSTIC_CODE_ON_MESSAGE_DUPLICATE_METHOD, null,
                                                    DiagnosticSeverity.Error));
                                            diagnostics.add(createDiagnostic(onMessageBinaryUsed, unit,
                                                    WebSocketConstants.DIAGNOSTIC_ON_MESSAGE_DUPLICATE_METHOD,
                                                    WebSocketConstants.DIAGNOSTIC_CODE_ON_MESSAGE_DUPLICATE_METHOD, null,
                                                    DiagnosticSeverity.Error));
                                        }
                                        onMessageBinaryUsed = annotation;
                                        break;
                                    case PONG:
                                        if (onMessagePongUsed != null) {
                                            diagnostics.add(createDiagnostic(annotation, unit,
                                                    WebSocketConstants.DIAGNOSTIC_ON_MESSAGE_DUPLICATE_METHOD,
                                                    WebSocketConstants.DIAGNOSTIC_CODE_ON_MESSAGE_DUPLICATE_METHOD, null,
                                                    DiagnosticSeverity.Error));
                                            diagnostics.add(createDiagnostic(onMessagePongUsed, unit,
                                                    WebSocketConstants.DIAGNOSTIC_ON_MESSAGE_DUPLICATE_METHOD,
                                                    WebSocketConstants.DIAGNOSTIC_CODE_ON_MESSAGE_DUPLICATE_METHOD, null,
                                                    DiagnosticSeverity.Error));
                                        }
                                        onMessagePongUsed = annotation;
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Create an error diagnostic if a ServerEndpoint annotation's URI contains relative
     * paths, missing a leading slash, or does not follow a valid level-1 template URI.
     */

    private void serverEndpointErrorCheck(PsiClass type, List<Diagnostic> diagnostics, PsiJavaFile unit) {
        PsiAnnotation[] annotations = type.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (isMatchedJavaElement(type, annotation.getQualifiedName(),
                    WebSocketConstants.SERVER_ENDPOINT_ANNOTATION)) {
                for (PsiNameValuePair annotationMemberValuePair : annotation.getParameterList().getAttributes()) {
                    String annoArgName = annotationMemberValuePair.getName();
                    if (annoArgName == null || annoArgName.equals(WebSocketConstants.ANNOTATION_VALUE)) {
                        String path = annotationMemberValuePair.getLiteralValue();
                        if (!JDTUtils.hasLeadingSlash(path)) {
                            diagnostics.add(createDiagnostic(annotation, unit,
                                    WebSocketConstants.DIAGNOSTIC_SERVER_ENDPOINT_NO_SLASH,
                                    WebSocketConstants.DIAGNOSTIC_SERVER_ENDPOINT, null, DiagnosticSeverity.Error));
                        }
                        if (hasRelativePathURIs(path)) {
                            diagnostics.add(createDiagnostic(annotation, unit,
                                    WebSocketConstants.DIAGNOSTIC_SERVER_ENDPOINT_RELATIVE,
                                    WebSocketConstants.DIAGNOSTIC_SERVER_ENDPOINT, null, DiagnosticSeverity.Error));
                        } else if (!JDTUtils.isValidLevel1URI(path)) {
                            diagnostics.add(createDiagnostic(annotation, unit,
                                    WebSocketConstants.DIAGNOSTIC_SERVER_ENDPOINT_NOT_LEVEL1,
                                    WebSocketConstants.DIAGNOSTIC_SERVER_ENDPOINT, null, DiagnosticSeverity.Error));
                        }
                        if (hasDuplicateURIVariables(path)) {
                            diagnostics.add(createDiagnostic(annotation, unit,
                                    WebSocketConstants.DIAGNOSTIC_SERVER_ENDPOINT_DUPLICATE_VAR,
                                    WebSocketConstants.DIAGNOSTIC_SERVER_ENDPOINT, null, DiagnosticSeverity.Error));
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds a WebSocket EndPoint annotation and extracts all variable parameters in
     * the EndPoint URI
     *
     * @param type representing the class
     * @return List of variable parameters in the EndPoint URI if one exists, null
     *         otherwise
     */
    private List<String> findAndProcessEndpointURI(PsiClass type) {
        String endpointURI = null;
        PsiAnnotation[] typeAnnotations = type.getAnnotations();
        String[] targetAnnotations = {WebSocketConstants.SERVER_ENDPOINT_ANNOTATION, WebSocketConstants.CLIENT_ENDPOINT_ANNOTATION};
        for (PsiAnnotation annotation : typeAnnotations) {
            String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(), targetAnnotations);
            if (matchedAnnotation != null) {
                PsiNameValuePair[] valuePairs = annotation.getParameterList().getAttributes();
                for (PsiNameValuePair pair : valuePairs) {
                    String annoArgName = pair.getName();
                    if (annoArgName == null || annoArgName.equals(WebSocketConstants.ANNOTATION_VALUE)) {
                        endpointURI = pair.getLiteralValue();
                    }
                }
            }
        }
        if (endpointURI == null) {
            return null;
        }
        List<String> endpointPathVars = new ArrayList<String>();
        String[] endpointParts = endpointURI.split(WebSocketConstants.URI_SEPARATOR);
        for (String part : endpointParts) {
            if (part.startsWith(WebSocketConstants.CURLY_BRACE_START)
                    && part.endsWith(WebSocketConstants.CURLY_BRACE_END)) {
                endpointPathVars.add(part.substring(1, part.length() - 1));
            }
        }
        return endpointPathVars;
    }
    /**
     * Check if valueClass is a wrapper object for a primitive value Based on
     * https://github.com/eclipse/lsp4mp/blob/9789a1a996811fade43029605c014c7825e8f1da/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/core/utils/JDTTypeUtils.java#L294-L298
     *
     * @param valueClass the resolved type of valueClass in string or the simple
     *                   type of valueClass
     * @return if valueClass is a wrapper object
     */
    private boolean isWrapper(String valueClass) {
        return WebSocketConstants.WRAPPER_OBJS.contains(valueClass)
                || WebSocketConstants.RAW_WRAPPER_OBJS.contains(valueClass);
    }

    /**
     * Checks if type is a WebSocket endpoint by meeting one of the 2 conditions
     * listed on
     * https://jakarta.ee/specifications/websocket/2.0/websocket-spec-2.0.html#applications
     * are met: class is annotated or class implements Endpoint class
     *
     * @param type the type representing the class
     * @return the conditions for a class to be a WebSocket endpoint
     */
    private HashMap<String, Boolean> isWSEndpoint(PsiClass type) {
        HashMap<String, Boolean> wsEndpoint = new HashMap<>();

        // check trivial case
        if (type == null) {
            wsEndpoint.put(WebSocketConstants.IS_ANNOTATION, false);
            wsEndpoint.put(WebSocketConstants.IS_SUPERCLASS, false);
            return wsEndpoint;
        }

        // Check that class follows
        // https://jakarta.ee/specifications/websocket/2.0/websocket-spec-2.0.html#applications
        List<String> endpointAnnotations = getMatchedJavaElementNames(type,
                Stream.of(type.getAnnotations()).map(annotation -> annotation.getQualifiedName()).toArray(String[]::new),
                WebSocketConstants.WS_ANNOTATION_CLASS);

        boolean useSuperclass = InheritanceUtil.isInheritor(type, WebSocketConstants.FQ_ENDPOINT_SUPERCLASS);

        wsEndpoint.put(WebSocketConstants.IS_ANNOTATION, (endpointAnnotations.size() > 0));
        wsEndpoint.put(WebSocketConstants.IS_SUPERCLASS, useSuperclass);

        return wsEndpoint;
    }
    private boolean isParamPath(PsiClass type, PsiParameter param) {
        PsiAnnotation[] allVariableAnnotations = param.getAnnotations();
        for (PsiAnnotation variableAnnotation : allVariableAnnotations) {
            if (isMatchedJavaElement(type, variableAnnotation.getQualifiedName(),
                    WebSocketConstants.PATH_PARAM_ANNOTATION)) {
                return true;
            }
        }
        return false;
    }
    private WebSocketConstants.MESSAGE_FORMAT getMessageFormat(String typeName, boolean longName) {
        if (longName) {
            switch (typeName) {
                case WebSocketConstants.STRING_CLASS_LONG:
                    return WebSocketConstants.MESSAGE_FORMAT.TEXT;
                case WebSocketConstants.READER_CLASS_LONG:
                    return WebSocketConstants.MESSAGE_FORMAT.TEXT;
                case WebSocketConstants.BYTEBUFFER_CLASS_LONG:
                    return WebSocketConstants.MESSAGE_FORMAT.BINARY;
                case WebSocketConstants.INPUTSTREAM_CLASS_LONG:
                    return WebSocketConstants.MESSAGE_FORMAT.BINARY;
                case WebSocketConstants.PONGMESSAGE_CLASS_LONG:
                    return WebSocketConstants.MESSAGE_FORMAT.PONG;
                default:
                    throw new IllegalArgumentException("Invalid message format type");
            }
        }
        switch (typeName) {
            case WebSocketConstants.STRING_CLASS_SHORT:
                return WebSocketConstants.MESSAGE_FORMAT.TEXT;
            case WebSocketConstants.READER_CLASS_SHORT:
                return WebSocketConstants.MESSAGE_FORMAT.TEXT;
            case WebSocketConstants.BYTEBUFFER_CLASS_SHORT:
                return WebSocketConstants.MESSAGE_FORMAT.BINARY;
            case WebSocketConstants.INPUTSTREAM_CLASS_SHORT:
                return WebSocketConstants.MESSAGE_FORMAT.BINARY;
            case WebSocketConstants.PONGMESSAGE_CLASS_SHORT:
                return WebSocketConstants.MESSAGE_FORMAT.PONG;
            default:
                throw new IllegalArgumentException("Invalid message format type");
        }
    }

    private String createParamTypeDiagMsg(Set<String> methodParamOptTypes, String methodAnnotTarget) {
        String paramMessage = String.join("\n- ", methodParamOptTypes);
        return String.format(WebSocketConstants.PARAM_TYPE_DIAG_MSG, "@" + methodAnnotTarget, paramMessage);
    }

    /**
     * Check if a URI string contains any sequence with //, /./, or /../
     *
     * @param uriString ServerEndpoint URI
     * @return if a URI has a relative path
     */
    private boolean hasRelativePathURIs(String uriString) {
        return uriString.matches(WebSocketConstants.REGEX_RELATIVE_PATHS);
    }

    /**
     * Check if a URI string has a duplicate variable
     *
     * @param uriString ServerEndpoint URI
     * @return if a URI has duplicate variables
     */
    private boolean hasDuplicateURIVariables(String uriString) {
        HashSet<String> variables = new HashSet<String>();
        for (String segment : uriString.split(WebSocketConstants.URI_SEPARATOR)) {
            if (segment.matches(WebSocketConstants.REGEX_URI_VARIABLE)) {
                String variable = segment.substring(1, segment.length() - 1);
                if (variables.contains(variable)) {
                    return true;
                } else {
                    variables.add(variable);
                }
            }
        }
        return false;
    }
}
