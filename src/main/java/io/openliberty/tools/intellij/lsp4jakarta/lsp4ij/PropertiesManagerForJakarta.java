/*******************************************************************************
 * Copyright (c) 2020,2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * IBM Corporation - handle Jakarta
 ******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations.AnnotationDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation.BeanValidationDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di.DependencyInjectionDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.Jax_RSClassDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.ResourceMethodDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb.JsonbDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonp.JsonpDiagnosticCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.PersistenceEntityDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.PersistenceMapKeyDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet.FilterDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet.ListenerDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet.ServletDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.websocket.WebSocketDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4mp.commons.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4jakarta.commons.JakartaDiagnosticsParams;

public class PropertiesManagerForJakarta {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesManagerForJakarta.class);

    private static final PropertiesManagerForJakarta INSTANCE = new PropertiesManagerForJakarta();

    public static PropertiesManagerForJakarta getInstance() {
        return INSTANCE;
    }

    private List<DiagnosticsCollector> diagnosticsCollectors = new ArrayList<>();

    private PropertiesManagerForJakarta() {
        diagnosticsCollectors.add(new ServletDiagnosticsCollector());
        diagnosticsCollectors.add(new AnnotationDiagnosticsCollector());
        diagnosticsCollectors.add(new FilterDiagnosticsCollector());
        diagnosticsCollectors.add(new ListenerDiagnosticsCollector());
        diagnosticsCollectors.add(new BeanValidationDiagnosticsCollector());
        diagnosticsCollectors.add(new PersistenceEntityDiagnosticsCollector());
        diagnosticsCollectors.add(new PersistenceMapKeyDiagnosticsCollector());
        diagnosticsCollectors.add(new ResourceMethodDiagnosticsCollector());
        diagnosticsCollectors.add(new Jax_RSClassDiagnosticsCollector());
        diagnosticsCollectors.add(new JsonbDiagnosticsCollector());
        diagnosticsCollectors.add(new ManagedBeanDiagnosticsCollector());
        diagnosticsCollectors.add(new DependencyInjectionDiagnosticsCollector());
        diagnosticsCollectors.add(new JsonpDiagnosticCollector());
        diagnosticsCollectors.add(new WebSocketDiagnosticsCollector());
    }

    /**
     * Returns diagnostics for the given uris list.
     *
     * @param params the diagnostics parameters
     * @param utils  the utilities class
     * @return diagnostics for the given uris list.
     */
    public List<PublishDiagnosticsParams> diagnostics(JakartaDiagnosticsParams params, IPsiUtils utils) {
        List<String> uris = params.getUris();
        if (uris == null) {
            return Collections.emptyList();
        }
        DocumentFormat documentFormat = params.getDocumentFormat();
        List<PublishDiagnosticsParams> publishDiagnostics = new ArrayList<PublishDiagnosticsParams>();
        for (String uri : uris) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            PublishDiagnosticsParams publishDiagnostic = new PublishDiagnosticsParams(uri, diagnostics);
            publishDiagnostics.add(publishDiagnostic);
            collectDiagnostics(uri, utils, documentFormat, diagnostics);
        }
        return publishDiagnostics;
    }

    private void collectDiagnostics(String uri, IPsiUtils utils, DocumentFormat documentFormat, List<Diagnostic> diagnostics) {
        PsiFile typeRoot = ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () -> resolveTypeRoot(uri, utils));
        if (typeRoot == null) {
            return;
        }

        try {
            if (typeRoot instanceof PsiJavaFile) {
                Module module = ApplicationManager.getApplication().runReadAction((ThrowableComputable<Module, IOException>) () -> utils.getModule(uri));
                DumbService.getInstance(module.getProject()).runReadActionInSmartMode(() -> {
                    PsiJavaFile unit = (PsiJavaFile) typeRoot;
                    for (DiagnosticsCollector collector : diagnosticsCollectors) {
                        collector.collectDiagnostics(unit, diagnostics);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @brief Gets all snippet contexts that exist in the current project classpath
     * @param uri             - String representing file from which to derive project
     *                        classpath
     * @param snippetContexts - get all the context fields from the snippets and
     *                        check if they exist in this method
     * @param project         - the IntelliJ project containing the uri
     * @return List<String>
     */
    public List<String> getExistingContextsFromClassPath(String uri, List<String> snippetContexts, Project project) {
        // ask the Java component if the classpath of the current module contains the specified Jakarta types.
        JavaPsiFacade javaFacade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
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

        // FOR NOW, append package name and class name to the list in order for LS to
        // resolve ${packagename} and ${classname} variables
        PsiFile typeRoot = ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () -> resolveTypeRoot(uri, project));
        DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            String className = "className";
            String packageName = "packageName";
            if (typeRoot instanceof PsiJavaFile) {
                PsiJavaFile javaFile = (PsiJavaFile)typeRoot;
                PsiClass[] classes = javaFile.getClasses();
                if (classes.length > 0) {
                    className = classes[0].getName();
                } else {
                    className = javaFile.getName();
                    if (className.endsWith(".java") == true) {
                        className = className.substring(0, className.length() - 5);
                    }
                }
                packageName = javaFile.getPackageName();
                if (packageName == null || packageName.isEmpty()) {
                    Path path = javaFile.getParent().getVirtualFile().toNioPath(); // f=/U/me/proj/src/main/java/pkg  not /cls.java
                    VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
                    for (VirtualFile vf : contentRoots) {
                        Path vfp = vf.toNioPath();
                        if (path.startsWith(vfp)) {
                            path = vfp.relativize(path); // remove the project part of the path
                            break;
                        }
                    }
                    packageName = path.toString().replace(File.separator, "."); // convert pkg1/pkg2
                }
            }
            validCtx.add(packageName);
            validCtx.add(className);
            });

        return validCtx;
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

    private static PsiFile resolveTypeRoot(String uri, Project project) {
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(project);
        return utils.resolveCompilationUnit(uri);
    }

}
