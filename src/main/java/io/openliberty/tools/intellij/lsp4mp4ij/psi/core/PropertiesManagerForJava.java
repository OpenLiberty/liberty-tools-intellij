/*******************************************************************************
 * Copyright (c) 2020, 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp4ij.psi.core;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codelens.IJavaCodeLensParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codelens.JavaCodeLensContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.completion.CompletionHandler;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.definition.IJavaDefinitionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.definition.JavaDefinitionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.symbols.IJavaWorkspaceSymbolsParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.DiagnosticsHandler;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.hover.IJavaHoverParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.hover.JavaHoverContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.java.codeaction.CodeActionHandler;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4mp.commons.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JDT quarkus manager for Java files.
 *
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManagerForJava.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManagerForJava.java</a>
 *
 */
public final class PropertiesManagerForJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesManagerForJava.class);

    private static final String GROUP_NAME = "mp";

    private static final PropertiesManagerForJava INSTANCE = new PropertiesManagerForJava();

    public static PropertiesManagerForJava getInstance() {
        return INSTANCE;
    }

    private final CompletionHandler completionHandler;

    private final CodeActionHandler codeActionHandler;

    private final DiagnosticsHandler diagnosticsHandler;

    private PropertiesManagerForJava() {
        this.completionHandler = new CompletionHandler(GROUP_NAME);
        this.codeActionHandler = new CodeActionHandler(GROUP_NAME);
        this.diagnosticsHandler = new DiagnosticsHandler(GROUP_NAME);
    }

    /**
     * Returns the Java file information (ex : package name) from the given file URI
     * and null otherwise.
     *
     * @param params  the file information parameters.
     * @param utils   the utilities class
     * @return the Java file information (ex : package name) from the given file URI
     *         and null otherwise.
     */
    public JavaFileInfo fileInfo(MicroProfileJavaFileInfoParams params, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<JavaFileInfo>) () -> {
            String uri = params.getUri();
            final PsiFile unit = utils.resolveCompilationUnit(uri);
            if (unit != null && unit.isValid() && unit instanceof PsiJavaFile) {
                JavaFileInfo fileInfo = new JavaFileInfo();
                String packageName = ((PsiJavaFile) unit).getPackageName();
                fileInfo.setPackageName(packageName);
                return fileInfo;
            }
            return null;
        });
    }

    /**
     * Returns the codelens list according the given codelens parameters.
     *
     * @param params  the codelens parameters
     * @param utils   the utilities class
     * @return the codelens list according the given codelens parameters.
     */
    public List<? extends CodeLens> codeLens(MicroProfileJavaCodeLensParams params, IPsiUtils utils,  ProgressIndicator monitor) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<? extends CodeLens>>) () -> {
            String uri = params.getUri();
            PsiFile typeRoot = resolveTypeRoot(uri, utils);
            if (typeRoot == null) {
                return Collections.emptyList();
            }
            List<CodeLens> lenses = new ArrayList<>();
            collectCodeLens(uri, typeRoot, utils, params, lenses, monitor);
            return lenses;
        });
    }

    private void collectCodeLens(String uri, PsiFile typeRoot, IPsiUtils utils, MicroProfileJavaCodeLensParams params,
                                 List<CodeLens> lenses,  ProgressIndicator monitor) {
        // Collect all adapted codeLens participant
        try {
            Module module = utils.getModule(uri);
            if (module == null) {
                return;
            }
            JavaCodeLensContext context = new JavaCodeLensContext(uri, typeRoot, utils, module, params);
            List<IJavaCodeLensParticipant> definitions = IJavaCodeLensParticipant.EP_NAME.getExtensionList()
                    .stream()
                    .filter(definition -> definition.isAdaptedForCodeLens(context, monitor))
                    .collect(Collectors.toList());
            if (definitions.isEmpty()) {
                return;
            }

            // Begin, collect, end participants
            definitions.forEach(definition -> definition.beginCodeLens(context, monitor));
            definitions.forEach(definition -> {
                List<CodeLens> collectedLenses = definition.collectCodeLens(context, monitor);
                if (collectedLenses != null && !collectedLenses.isEmpty()) {
                    lenses.addAll(collectedLenses);
                }
            });
            definitions.forEach(definition -> definition.endCodeLens(context, monitor));
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns the CompletionItems given the completion item params
     *
     * @param params  the completion item params
     * @param utils   the IJDTUtils
     * @return the CompletionItems for the given the completion item params
     */
    public CompletionList completion(MicroProfileJavaCompletionParams params, IPsiUtils utils) {
        return completionHandler.completion(params, utils);
    }

    /**
     * Returns the definition list according the given definition parameters.
     *
     * @param params  the definition parameters
     * @param utils   the utilities class
     * @return the definition list according the given definition parameters.
     */
    public List<MicroProfileDefinition> definition(MicroProfileJavaDefinitionParams params, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<MicroProfileDefinition>>)() -> {
            String uri = params.getUri();
            PsiFile typeRoot = resolveTypeRoot(uri, utils);
            if (typeRoot == null) {
                return Collections.emptyList();
            }

            Position hyperlinkedPosition = params.getPosition();
            int definitionOffset = utils.toOffset(typeRoot, hyperlinkedPosition.getLine(),
                    hyperlinkedPosition.getCharacter());
            PsiElement hyperlinkedElement = getHoveredElement(typeRoot, definitionOffset);

            List<MicroProfileDefinition> locations = new ArrayList<>();
            collectDefinition(uri, typeRoot, hyperlinkedElement, utils, hyperlinkedPosition, locations);
            return locations;
        });
    }

    private void collectDefinition(String uri, PsiFile typeRoot, PsiElement hyperlinkedElement, IPsiUtils utils,
                                   Position hyperlinkedPosition, List<MicroProfileDefinition> locations) {
        VirtualFile file = null;
        try {
            file = utils.findFile(uri);
            if (file != null) {
                Module module = utils.getModule(file);
                if (module != null) {
                    // Collect all adapted definition participant
                    JavaDefinitionContext context = new JavaDefinitionContext(uri, typeRoot, utils, module,
                            hyperlinkedElement, hyperlinkedPosition);
                    List<IJavaDefinitionParticipant> definitions = IJavaDefinitionParticipant.EP_NAME.getExtensionList()
                            .stream()
                            .filter(definition -> definition.isAdaptedForDefinition(context))
                            .toList();
                    if (definitions.isEmpty()) {
                        return;
                    }

                    // Begin, collect, end participants
                    definitions.forEach(definition -> definition.beginDefinition(context));
                    definitions.forEach(definition -> {
                        List<MicroProfileDefinition> collectedDefinitions = definition.collectDefinitions(context);
                        if (collectedDefinitions != null && !collectedDefinitions.isEmpty()) {
                            locations.addAll(collectedDefinitions);
                        }
                    });
                    definitions.forEach(definition -> definition.endDefinition(context));
                }
            }
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }


    /**
     * Returns diagnostics for the given uris list.
     *
     * @param params the diagnostics parameters
     * @param utils  the utilities class
     * @return diagnostics for the given uris list.
     */
    public List<PublishDiagnosticsParams> diagnostics(MicroProfileJavaDiagnosticsParams params, IPsiUtils utils) {
        return diagnosticsHandler.collectDiagnostics(params, utils);
    }

    /**
     * Returns the hover information according to the given <code>params</code>
     *
     * @param params  the hover parameters
     * @param utils   the utilities class
     * @return the hover information according to the given <code>params</code>
     */
    public Hover hover(MicroProfileJavaHoverParams params, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<Hover>) () -> {
            String uri = params.getUri();
            PsiFile typeRoot = resolveTypeRoot(uri, utils);
            if (typeRoot == null) {
                return null;
            }
            Document document = PsiDocumentManager.getInstance(typeRoot.getProject()).getDocument(typeRoot);
            if (document == null) {
                return null;
            }
            Position hoverPosition = params.getPosition();
            int hoveredOffset = utils.toOffset(document, hoverPosition.getLine(), hoverPosition.getCharacter());
            PsiElement hoverElement = getHoveredElement(typeRoot, hoveredOffset);
            if (hoverElement == null) return null;

            DocumentFormat documentFormat = params.getDocumentFormat();
            boolean surroundEqualsWithSpaces = params.isSurroundEqualsWithSpaces();
            List<Hover> hovers = new ArrayList<>();
            collectHover(uri, typeRoot, hoverElement, utils, hoverPosition, documentFormat, surroundEqualsWithSpaces,
                    hovers);
            if (hovers.isEmpty()) {
                return null;
            }
            // TODO : aggregate the hover
            return hovers.get(0);
        });
    }

    /**
     * Returns the cursor context for the given file and cursor position.
     *
     * @param params  the completion params that provide the file and cursor
     *                position to get the context for
     * @param utils   the jdt utils
     * @return the cursor context for the given file and cursor position
     */
    public JavaCursorContextResult javaCursorContext(MicroProfileJavaCompletionParams params, IPsiUtils utils) {
        return completionHandler.javaCursorContext(params, utils);
    }

    @Nullable
    private PsiElement getHoveredElement(PsiFile typeRoot, int offset) {
        PsiElement hoverElement = typeRoot.findElementAt(offset);
        if (hoverElement == null) {
            return null;
        }
        hoverElement = PsiTreeUtil.getParentOfType(hoverElement, PsiModifierListOwner.class);
        if (hoverElement instanceof PsiMethod) {
            hoverElement = getHoveredMethodParameter((PsiMethod) hoverElement, offset);
        }
        return hoverElement;
    }

    /**
     * Returns the parameter element from the given <code>method</code> that
     * contains the given <code>offset</code>.
     *
     * Returns the given <code>method</code> if the correct parameter element cannot
     * be found
     *
     * @param method the method
     * @param offset the offset
     * @return the parameter element from the given <code>method</code> that
     *         contains the given <code>offset</code>
     */
    private PsiElement getHoveredMethodParameter(PsiMethod method, int offset) {
        JvmParameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] instanceof PsiParameter) {
                int start = ((PsiParameter)parameters[i] ).getStartOffsetInParent();
                int end = start + ((PsiParameter) parameters[i]).getTextLength();
                if (start <= offset && offset <= end) {
                    return (PsiElement) parameters[i];
                }
            }
        }
        return method;

    }

    private void collectHover(String uri, PsiFile typeRoot, PsiElement hoverElement, IPsiUtils utils,
                              Position hoverPosition, DocumentFormat documentFormat, boolean surroundEqualsWithSpaces,
                              List<Hover> hovers) {
        try {
            VirtualFile file = utils.findFile(uri);
            if (file != null) {
                Module module = utils.getModule(file);
                if (module != null) {
                    // Collect all adapted hover participant
                    JavaHoverContext context = new JavaHoverContext(uri, typeRoot, utils, module, hoverElement, hoverPosition,
                            documentFormat, surroundEqualsWithSpaces);
                    List<IJavaHoverParticipant> definitions = IJavaHoverParticipant.EP_NAME.getExtensionList().stream()
                            .filter(definition -> definition.isAdaptedForHover(context)).collect(Collectors.toList());
                    if (definitions.isEmpty()) {
                        return;
                    }

                    // Begin, collect, end participants
                    definitions.forEach(definition -> definition.beginHover(context));
                    definitions.forEach(definition -> {
                        Hover hover = definition.collectHover(context);
                        if (hover != null) {
                            hovers.add(hover);
                        }
                    });
                    definitions.forEach(definition -> definition.endHover(context));
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }


    /**
     * Given the uri returns a {@link PsiFile}. May return null if it can not
     * associate the uri with a Java file ot class file.
     *
     * @param uri
     * @param utils   JDT LS utilities
     * @return compilation unit
     */
    private static PsiFile resolveTypeRoot(String uri, IPsiUtils utils) {
        return utils.resolveCompilationUnit(uri);
    }

    /**
     * Returns the codeAction list according the given codeAction parameters.
     *
     * @param params  the codeAction parameters
     * @param utils   the utilities class
     * @return the codeAction list according the given codeAction parameters.
     */
    public List<? extends CodeAction> codeAction(MicroProfileJavaCodeActionParams params, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<? extends CodeAction>>) () -> {
            return codeActionHandler.codeAction(params, utils);
        });
    }

    /**
     * Returns the codeAction list according the given codeAction parameters.
     *
     * @param unresolved the CodeAction to resolve
     * @param utils      the utilities class
     * @return the codeAction list according the given codeAction parameters.
     */
    public CodeAction resolveCodeAction(CodeAction unresolved, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<CodeAction>) () -> {
            return codeActionHandler.resolveCodeAction(unresolved, utils);
        });
    }

    /**
     * Returns the workspace symbols for the given java project.
     *
     * @param projectUri the uri of the java project
     * @param utils      the JDT utils
     * @param monitor    the progress monitor
     * @return the workspace symbols for the given java project
     */
    public List<WorkspaceSymbol> workspaceSymbols(String projectUri, IPsiUtils utils, ProgressIndicator monitor) {
        List<WorkspaceSymbol> symbols = new ArrayList<>();
        Module module = getModule(projectUri, utils);
        if (module != null) {
            collectWorkspaceSymbols(module, utils, symbols, monitor);
        }
        return symbols;
    }

    private static @Nullable Module getModule(String uri, IPsiUtils utils) {
        Module[] modules = ModuleManager.getInstance(utils.getProject()).getModules();
        for (Module module : modules) {
            if (uri.equals(module.getName())) {
                return module;
            }
        }
        return null;
    }

    private void collectWorkspaceSymbols(Module project, IPsiUtils utils, List<WorkspaceSymbol> symbols,
                                         ProgressIndicator monitor) {
        if (monitor.isCanceled()) {
            return;
        }

        List<IJavaWorkspaceSymbolsParticipant> definitions = IJavaWorkspaceSymbolsParticipant.EP_NAME.getExtensionList();
        if (definitions.isEmpty()) {
            return;
        }
        definitions.forEach(definition -> definition.collectSymbols(project, utils, symbols, monitor));
    }
}
