/*******************************************************************************
 * Copyright (c) 2019, 2022 Red Hat, Inc.
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PropertiesManagerForJakarta;
import io.openliberty.tools.intellij.lsp4mp.MicroProfileProjectService;
import io.openliberty.tools.intellij.lsp4mp.lsp4ij.LanguageClientImpl;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4jakarta.api.JakartaLanguageClientAPI;
import org.eclipse.lsp4jakarta.commons.JakartaClasspathParams;
import org.eclipse.lsp4jakarta.commons.JakartaDiagnosticsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusLanguageClient.java
 * to start LSP4MP, Language Server for MicroProfile
 */
public class JakartaLanguageClient extends LanguageClientImpl implements JakartaLanguageClientAPI, MicroProfileProjectService.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(JakartaLanguageClient.class);

  public JakartaLanguageClient(Project project) {
    super(project);
  }

  // Support the message "jakarta/java/classpath"
  @Override
  public CompletableFuture<List<String>> getContextBasedFilter(JakartaClasspathParams classpathParams) {
    List<String> snippetContexts = classpathParams.getSnippetCtx();
    // ask the Java component if the classpath of the current module contains the specified Jakarta types.
    JavaPsiFacade javaFacade = JavaPsiFacade.getInstance(getProject());
    GlobalSearchScope scope = GlobalSearchScope.allScope(getProject());
    List<String> validCtx = new ArrayList<String>();
    if (javaFacade != null && scope != null) {
      for (String typeCtx : snippetContexts) {
        Object type = ApplicationManager.getApplication().runReadAction((Computable<Object>) () -> javaFacade.findClass(typeCtx, scope));
        validCtx.add(type != null ? typeCtx : null); // list will be the same size as input
      }
    } else {
      // Error: none of these contexts will add to the completions
      for (String typeCtx : snippetContexts) {
        validCtx.add(null); // list will be the same size as input
      }
    }
    return CompletableFuture.completedFuture(validCtx);
  }

  // Support the message "jakarta/java/diagnostics"
  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(JakartaDiagnosticsParams jakartaParams) {
    IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
    List<PublishDiagnosticsParams> diagnostics = PropertiesManagerForJakarta.getInstance().diagnostics(jakartaParams, utils);
    return CompletableFuture.completedFuture(diagnostics);
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
}
