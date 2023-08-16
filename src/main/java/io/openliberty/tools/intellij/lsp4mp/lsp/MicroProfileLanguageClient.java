/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp.lsp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.ProjectLabelManager;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import io.openliberty.tools.intellij.lsp4mp.MicroProfileModuleUtil;
import io.openliberty.tools.intellij.lsp4mp.MicroProfileProjectService;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4ij.IndexAwareLanguageClient;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4mp.commons.*;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.utils.JSONUtility;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageClientAPI;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusLanguageClient.java
 * to start LSP4MP, Language Server for MicroProfile
 */
public class MicroProfileLanguageClient extends IndexAwareLanguageClient implements MicroProfileLanguageClientAPI, MicroProfileProjectService.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(MicroProfileLanguageClient.class);
  private static final String JAVA_FILE_EXTENSION = "java";

  private final MessageBusConnection connection;

  public MicroProfileLanguageClient(Project project) {
    super(project);
    connection = project.getMessageBus().connect(project);
    connection.subscribe(MicroProfileProjectService.TOPIC, this);
    MicroProfileProjectService.getInstance(project);
  }

  private void sendPropertiesChangeEvent(List<MicroProfilePropertiesScope> scope, Set<String> uris) {
    MicroProfileLanguageServerAPI server = (MicroProfileLanguageServerAPI) getLanguageServer();
    if (server != null) {
      MicroProfilePropertiesChangeEvent event = new MicroProfilePropertiesChangeEvent();
      event.setType(scope);
      event.setProjectURIs(uris);
      server.propertiesChanged(event);
    }
  }

  @Override
  public void libraryUpdated(Library library) {
    sendPropertiesChangeEvent(Collections.singletonList(MicroProfilePropertiesScope.dependencies), MicroProfileModuleUtil.getModulesURIs(getProject()));
  }

  @Override
  public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
    List<Pair<String,MicroProfilePropertiesScope>> info = sources.stream().
            filter(pair -> isJavaFile(pair.getRight()) || isConfigSource(pair.getRight(), pair.getLeft())).
            map(pair -> Pair.of(PsiUtilsLSImpl.getProjectURI(pair.getLeft()), getScope(pair.getRight()))).
            collect(Collectors.toList());
    if (!info.isEmpty()) {
      sendPropertiesChangeEvent(info.stream().map(Pair::getRight).collect(Collectors.toList()), info.stream().map(Pair::getLeft).collect(Collectors.toSet()));
    }
  }

  private MicroProfilePropertiesScope getScope(VirtualFile file) {
    return isJavaFile(file)?MicroProfilePropertiesScope.sources:MicroProfilePropertiesScope.configfiles;
  }

  private boolean isJavaFile(VirtualFile file) {
    return JAVA_FILE_EXTENSION.equals(file.getExtension());
  }

  private boolean isConfigSource(VirtualFile file, Module project) {
    return PsiMicroProfileProjectManager.getInstance(project.getProject()).isConfigSource(file);
  }

  @Override
  public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
    return runAsBackground("Computing project information", monitor -> PropertiesManager.getInstance().getMicroProfileProjectInfo(params, PsiUtilsLSImpl.getInstance(getProject()), monitor));
  }

  @Override
  public CompletableFuture<Hover> getJavaHover(MicroProfileJavaHoverParams javaParams) {
    return runAsBackground("Computing Java hover", monitor -> PropertiesManagerForJava.getInstance().hover(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(MicroProfileJavaDiagnosticsParams javaParams) {
    return runAsBackground("Computing Java diagnostics", monitor -> PropertiesManagerForJava.getInstance().diagnostics(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<Location> getPropertyDefinition(MicroProfilePropertyDefinitionParams params) {
    return runAsBackground("Computing property definition", monitor -> PropertiesManager.getInstance().findPropertyLocation(params, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectLabels(MicroProfileJavaProjectLabelsParams javaParams) {
    return runAsBackground("Computing Java projects labels", monitor -> ProjectLabelManager.getInstance().getProjectLabelInfo(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<JavaFileInfo> getJavaFileInfo(MicroProfileJavaFileInfoParams javaParams) {
    return runAsBackground("Computing Java file info", monitor -> PropertiesManagerForJava.getInstance().fileInfo(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<MicroProfileJavaCompletionResult> getJavaCompletion(MicroProfileJavaCompletionParams javaParams) {
    return runAsBackground("Computing Java completion", monitor -> {
      IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
      CompletionList completionList = PropertiesManagerForJava.getInstance().completion(javaParams, utils);
      JavaCursorContextResult cursorContext = PropertiesManagerForJava.getInstance().javaCursorContext(javaParams, utils);
      return new MicroProfileJavaCompletionResult(completionList, cursorContext);
    });
  }

  @Override
  public CompletableFuture<List<? extends CodeLens>> getJavaCodelens(MicroProfileJavaCodeLensParams javaParams) {
    return runAsBackground("Computing Java codelens", monitor -> PropertiesManagerForJava.getInstance().codeLens(javaParams, PsiUtilsLSImpl.getInstance(getProject()), monitor));
  }
  
  @Override
  public CompletableFuture<List<CodeAction>> getJavaCodeAction(MicroProfileJavaCodeActionParams javaParams) {
    return runAsBackground("Computing Java code actions", monitor -> (List<CodeAction>) PropertiesManagerForJava.getInstance().codeAction(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
    return runAsBackground("Computing Java resolve code actions", monitor -> {
      CodeActionResolveData data = JSONUtility.toModel(unresolved.getData(), CodeActionResolveData.class);
      unresolved.setData(data);
      return (CodeAction) PropertiesManagerForJava.getInstance().resolveCodeAction(unresolved, PsiUtilsLSImpl.getInstance(getProject()));
    });
  }

  @Override
  public CompletableFuture<List<ProjectLabelInfoEntry>> getAllJavaProjectLabels() {
    return runAsBackground("Computing All Java projects labels", monitor -> ProjectLabelManager.getInstance().getProjectLabelInfo(PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<List<MicroProfileDefinition>> getJavaDefinition(MicroProfileJavaDefinitionParams javaParams) {
    return runAsBackground("Computing Java definitions", monitor -> PropertiesManagerForJava.getInstance().definition(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<JavaCursorContextResult> getJavaCursorContext(MicroProfileJavaCompletionParams params) {
    return runAsBackground("Computing Java Cursor context", monitor -> PropertiesManagerForJava.getInstance().javaCursorContext(params, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<List<SymbolInformation>> getJavaWorkspaceSymbols(String projectUri) {
    //Workspace symbols not supported yet https://github.com/redhat-developer/intellij-quarkus/issues/808
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<String> getPropertyDocumentation(MicroProfilePropertyDocumentationParams params) {
    // Requires porting https://github.com/eclipse/lsp4mp/issues/321 / https://github.com/eclipse/lsp4mp/pull/329
    return CompletableFuture.completedFuture(null);
  }
}
