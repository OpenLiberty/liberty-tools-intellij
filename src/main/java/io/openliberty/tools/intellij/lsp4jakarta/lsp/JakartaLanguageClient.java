/*******************************************************************************
 * Copyright (c) 2019, 2023 Red Hat, Inc. and others
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * IBM Corporation
 ******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PropertiesManagerForJakarta;
import io.openliberty.tools.intellij.lsp4mp.MicroProfileProjectService;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.ProjectLabelManager;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.*;
import org.eclipse.lsp4jakarta.ls.api.JakartaLanguageClientAPI;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaProjectLabelsParams;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.utils.JSONUtility;
import org.microshed.lsp4ij.client.CoalesceByKey;
import org.microshed.lsp4ij.client.IndexAwareLanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusLanguageClient.java
 * to match LSP4MP, Language Server for MicroProfile
 */
public class JakartaLanguageClient extends IndexAwareLanguageClient implements JakartaLanguageClientAPI, MicroProfileProjectService.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(JakartaLanguageClient.class);

  public JakartaLanguageClient(Project project) {
    super(project);
  }

  // Support the message "jakarta/java/classpath"
  /** public CompletableFuture<List<String>> getContextBasedFilter(JakartaClasspathParams classpathParams) {
    String uri = classpathParams.getUri();
    List<String> snippetContexts = classpathParams.getSnippetCtx();
    Project project = getProject();
    var coalesceBy = new CoalesceByKey("jakarta/java/classpath", uri, snippetContexts);
    return runAsBackground("Computing Jakarta context",
            monitor -> PropertiesManagerForJakarta.getInstance().getExistingContextsFromClassPath(uri, snippetContexts, project), coalesceBy);
  } **/

  // Support the message "jakarta/java/cursorcontext"
  /** public CompletableFuture<JavaCursorContextResult> getJavaCursorContext(JakartaJavaCompletionParams params) {
    var coalesceBy = new CoalesceByKey("jakarta/java/cursorcontext", params.getUri(), params.getPosition());
    return runAsBackground("Computing Java cursor context",
            monitor -> {
              IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
              return PropertiesManagerForJakarta.getInstance().javaCursorContext(params, utils);
            }, coalesceBy);
  } **/

  // Support the message "jakarta/java/diagnostics"
  /** public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(JakartaDiagnosticsParams jakartaParams) {
    var coalesceBy = new CoalesceByKey("jakarta/java/diagnostics", jakartaParams.getUris());
    IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
    return runAsBackground("Computing Jakarta Java diagnostics",
            monitor -> PropertiesManagerForJakarta.getInstance().diagnostics(jakartaParams, utils), coalesceBy);
  } **/

  // Support the message "jakarta/java/codeaction
  /** public CompletableFuture<List<CodeAction>> getCodeAction(JakartaJavaCodeActionParams params) {
    IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
    var coalesceBy = new CoalesceByKey("jakarta/java/codeAction", params.getUri());
    return runAsBackground("Computing Jakarta code actions",
            monitor -> (List<CodeAction>) PropertiesManagerForJakarta.getInstance().getCodeAction(params, utils), coalesceBy);
  } **/

  // Support the message "jakarta/java/diagnostics"
  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(JakartaJavaDiagnosticsParams jakartaJavaDiagnosticsParams) {
    var coalesceBy = new CoalesceByKey("jakarta/java/diagnostics", jakartaJavaDiagnosticsParams.getUris());
    IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
    return runAsBackground("Computing Jakarta Java diagnostics",
            monitor -> PropertiesManagerForJakarta.getInstance().diagnostics(jakartaJavaDiagnosticsParams, utils), coalesceBy);
  }

  // Support the message "jakarta/java/codeaction"
  @Override
  public CompletableFuture<List<CodeAction>> getJavaCodeAction(JakartaJavaCodeActionParams jakartaJavaCodeActionParams) {
    final IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
    var coalesceBy = new CoalesceByKey("jakarta/java/codeAction", jakartaJavaCodeActionParams.getUri());
    return runAsBackground("Computing Jakarta code actions",
            monitor -> (List<CodeAction>) PropertiesManagerForJakarta.getInstance().getCodeAction(jakartaJavaCodeActionParams, utils), coalesceBy);
  }

  // Support the message "jakarta/java/resolveCodeAction"
  @Override
  public CompletableFuture<CodeAction> resolveCodeAction(CodeAction codeAction) {
    final IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
    var coalesceBy = new CoalesceByKey("jakarta/java/resolveCodeAction");
    return runAsBackground("Computing Java resolve code actions", monitor -> {
      CodeActionResolveData data = JSONUtility.toModel(codeAction.getData(), CodeActionResolveData.class);
      codeAction.setData(data);
      return PropertiesManagerForJakarta.getInstance().resolveCodeAction(codeAction, utils);
    }, coalesceBy);
  }

  @Override
  public CompletableFuture<JakartaJavaCompletionResult> getJavaCompletion(JakartaJavaCompletionParams jakartaJavaCompletionParams) {
    return null;
  }

  // Support the message "jakarta/java/projectLabels"
  @Override
  public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectLabels(JakartaJavaProjectLabelsParams jakartaJavaProjectLabelsParams) {
    final IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
    var coalesceBy = new CoalesceByKey("jakarta/java/projectLabels",
            jakartaJavaProjectLabelsParams.getUri(), jakartaJavaProjectLabelsParams.getTypes());
    return runAsBackground("Computing Java projects labels",
            monitor -> adapt(ProjectLabelManager.getInstance().getProjectLabelInfo(adapt(jakartaJavaProjectLabelsParams), utils)), coalesceBy);
  }

  // Support the message "jakarta/java/workspaceLabels"
  @Override
  public CompletableFuture<List<ProjectLabelInfoEntry>> getAllJavaProjectLabels() {
    final IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
    var coalesceBy = new CoalesceByKey("jakarta/java/workspaceLabels");
    return runAsBackground("Computing All Java projects labels",
            monitor -> adapt(ProjectLabelManager.getInstance().getProjectLabelInfo(utils)), coalesceBy);
  }

  // Support the message "jakarta/java/fileInfo"
  @Override
  public CompletableFuture<JakartaJavaFileInfo> getJavaFileInfo(JakartaJavaFileInfoParams jakartaJavaFileInfoParams) {
    final IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
    var coalesceBy = new CoalesceByKey("jakarta/java/fileInfo", jakartaJavaFileInfoParams.getUri());
    return runAsBackground("Computing Java file info",
            monitor -> adapt(PropertiesManagerForJava.getInstance().fileInfo(adapt(jakartaJavaFileInfoParams), utils)), coalesceBy);
  }

  @Override
  public void libraryUpdated(Library library) {
    // not needed for Jakarta LS
  }

  @Override
  public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
    int i = 0;
    // not needed for Jakarta LS
  }

  // REVISIT: The "adapt" methods in this class are being used to convert between data structures
  // from LSP4MP and LSPJakarta that are otherwise identical except for their class names. Once
  // LSP4MP and LSP4Jakarta have a common/unified client API, the "adapt" methods can be removed.

  private List<ProjectLabelInfoEntry> adapt(List<org.eclipse.lsp4mp.commons.ProjectLabelInfoEntry> mpEntries) {
    if (mpEntries != null) {
      final List<ProjectLabelInfoEntry> jakartaEntries = new ArrayList<>();
      mpEntries.forEach(x -> jakartaEntries.add(adapt(x)));
      return jakartaEntries;
    }
    return null;
  }

  private ProjectLabelInfoEntry adapt(org.eclipse.lsp4mp.commons.ProjectLabelInfoEntry mpEntry) {
    return new ProjectLabelInfoEntry(mpEntry.getUri(), mpEntry.getName(), mpEntry.getLabels());
  }

  private MicroProfileJavaProjectLabelsParams adapt(JakartaJavaProjectLabelsParams params) {
    if (params != null) {
      final MicroProfileJavaProjectLabelsParams mpParams = new MicroProfileJavaProjectLabelsParams();
      mpParams.setUri(params.getUri());
      mpParams.setTypes(params.getTypes());
      return mpParams;
    }
    return null;
  }

  private JakartaJavaFileInfo adapt(JavaFileInfo fileInfo) {
    if (fileInfo != null) {
      final JakartaJavaFileInfo jakartaFileInfo = new JakartaJavaFileInfo();
      jakartaFileInfo.setPackageName(fileInfo.getPackageName());
      return jakartaFileInfo;
    }
    return null;
  }

  private MicroProfileJavaFileInfoParams adapt(JakartaJavaFileInfoParams params) {
    if (params != null) {
      final MicroProfileJavaFileInfoParams mpParams = new MicroProfileJavaFileInfoParams();
      mpParams.setUri(params.getUri());
      return mpParams;
    }
    return null;
  }
}
