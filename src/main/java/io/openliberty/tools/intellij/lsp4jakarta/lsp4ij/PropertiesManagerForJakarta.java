/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat, Inc.
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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.JakartaCodeActionHandler;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.DiagnosticsHandler;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.*;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PropertiesManagerForJakarta {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesManagerForJakarta.class);

    private static final PropertiesManagerForJakarta INSTANCE = new PropertiesManagerForJakarta();

    public static PropertiesManagerForJakarta getInstance() {
        return INSTANCE;
    }

    private JakartaCodeActionHandler codeActionHandler = new JakartaCodeActionHandler();

    private final DiagnosticsHandler diagnosticsHandler;

    private PropertiesManagerForJakarta() {
        codeActionHandler = new JakartaCodeActionHandler();
        diagnosticsHandler = new DiagnosticsHandler("jakarta");
    }

    /**
     * Returns diagnostics for the given uris list.
     *
     * @param params the diagnostics parameters
     * @param utils  the utilities class
     * @return diagnostics for the given uris list.
     */
    public List<PublishDiagnosticsParams> diagnostics(JakartaDiagnosticsParams params, IPsiUtils utils) {
        return diagnosticsHandler.collectDiagnostics(adapt(params), utils);
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
     * Returns the cursor context for the given file and cursor position.
     *
     * @param params  the completion params that provide the file and cursor
     *                position to get the context for
     * @param utils   the jdt utils
     * @return the cursor context for the given file and cursor position
     */
    public JavaCursorContextResult javaCursorContext(JakartaJavaCompletionParams params, IPsiUtils utils) {
        JavaCursorContextResult result = ApplicationManager.getApplication().runReadAction((Computable<JavaCursorContextResult>) () -> {
            String uri = params.getUri();
            PsiFile typeRoot = resolveTypeRoot(uri, utils);
            if (!(typeRoot instanceof PsiJavaFile)) {
                return new JavaCursorContextResult(JavaCursorContextKind.IN_EMPTY_FILE, "");
            }
            Document document = PsiDocumentManager.getInstance(typeRoot.getProject()).getDocument(typeRoot);
            if (document == null) {
                return new JavaCursorContextResult(JavaCursorContextKind.IN_EMPTY_FILE, "");
            }
            Position completionPosition = params.getPosition();
            int completionOffset = utils.toOffset(document, completionPosition.getLine(), completionPosition.getCharacter());

            //CompilationUnit ast = ASTResolving.createQuickFixAST((ICompilationUnit) typeRoot);

            JavaCursorContextKind kind = getJavaCursorContextKind((PsiJavaFile) typeRoot, completionOffset);
            String prefix = getJavaCursorPrefix(document, completionOffset);
            return new JavaCursorContextResult(kind, prefix);
        });

        return result;
    }

    private static JavaCursorContextKind getJavaCursorContextKind(PsiJavaFile javaFile, int completionOffset) {
        if (javaFile.getClasses().length == 0) {
            return JavaCursorContextKind.IN_EMPTY_FILE;
        }

        PsiElement element = javaFile.findElementAt(completionOffset);
        PsiElement parent = PsiTreeUtil.getParentOfType(element, PsiModifierListOwner.class);

        if (parent == null) {
            // We are likely before or after the class declaration
            PsiElement firstClass = javaFile.getClasses()[0];

            if (completionOffset <= firstClass.getTextOffset()) {
                return JavaCursorContextKind.BEFORE_CLASS;
            }

            return JavaCursorContextKind.NONE;
        }

        if (parent instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) parent;
            return getContextKindFromClass(completionOffset, psiClass, element);
        }

        if (parent instanceof PsiAnnotation) {
            PsiAnnotation psiAnnotation = (PsiAnnotation) parent;
            PsiAnnotationOwner annotationOwner = psiAnnotation.getOwner();
            if (annotationOwner instanceof PsiClass) {
                return (psiAnnotation.getStartOffsetInParent() == 0)? JavaCursorContextKind.BEFORE_CLASS:JavaCursorContextKind.IN_CLASS_ANNOTATIONS;
            }
            if (annotationOwner instanceof PsiMethod){
                return (psiAnnotation.getStartOffsetInParent() == 0)? JavaCursorContextKind.BEFORE_METHOD:JavaCursorContextKind.IN_METHOD_ANNOTATIONS;
            }
            if (annotationOwner instanceof PsiField) {
                return (psiAnnotation.getStartOffsetInParent() == 0)? JavaCursorContextKind.BEFORE_FIELD:JavaCursorContextKind.IN_FIELD_ANNOTATIONS;
            }
        }

        if (parent instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) parent;
            if (completionOffset == psiMethod.getTextRange().getStartOffset()) {
                return JavaCursorContextKind.BEFORE_METHOD;
            }
            int methodStartOffset = getMethodStartOffset(psiMethod);
            if (completionOffset <= methodStartOffset) {
                if (psiMethod.getAnnotations().length > 0) {
                    return JavaCursorContextKind.IN_METHOD_ANNOTATIONS;
                }
                return JavaCursorContextKind.BEFORE_METHOD;
            }
        }

        if (parent instanceof PsiField) {
            PsiField psiField = (PsiField) parent;
            if (completionOffset == psiField.getTextRange().getStartOffset()) {
                return JavaCursorContextKind.BEFORE_FIELD;
            }
            int fieldStartOffset = getFieldStartOffset(psiField);
            if (completionOffset <= fieldStartOffset) {
                if (psiField.getAnnotations().length > 0) {
                    return JavaCursorContextKind.IN_FIELD_ANNOTATIONS;
                }
                return JavaCursorContextKind.BEFORE_FIELD;
            }
        }

        return JavaCursorContextKind.NONE;
    }

    private static JavaCursorContextKind getContextKindFromClass(int completionOffset, PsiClass psiClass, PsiElement element) {
        if (completionOffset <= psiClass.getTextRange().getStartOffset()) {
            return JavaCursorContextKind.BEFORE_CLASS;
        }
        int classStartOffset = getClassStartOffset(psiClass);
        if (completionOffset <= classStartOffset) {
            if (psiClass.getAnnotations().length > 0) {
                return JavaCursorContextKind.IN_CLASS_ANNOTATIONS;
            }
            return JavaCursorContextKind.BEFORE_CLASS;
        }

        PsiElement nextElement = element.getNextSibling();

        if (nextElement instanceof  PsiField) {
            return JavaCursorContextKind.BEFORE_FIELD;
        }
        if (nextElement instanceof  PsiMethod) {
            return JavaCursorContextKind.BEFORE_METHOD;
        }
        if (nextElement instanceof  PsiClass) {
            return JavaCursorContextKind.BEFORE_CLASS;
        }

        return JavaCursorContextKind.IN_CLASS;
    }
    private static int getClassStartOffset(PsiClass psiClass) {
        int startOffset = psiClass.getTextOffset();

        int modifierStartOffset = getFirstKeywordOffset(psiClass);
        if (modifierStartOffset > -1) {
            return Math.min(startOffset, modifierStartOffset);
        }
        return startOffset;
    }

    private static int getMethodStartOffset(PsiMethod psiMethod) {
        int startOffset = psiMethod.getTextOffset();

        int modifierStartOffset = getFirstKeywordOffset(psiMethod);
        if (modifierStartOffset > -1) {
            return Math.min(startOffset, modifierStartOffset);
        }

        PsiTypeElement returnTypeElement = psiMethod.getReturnTypeElement();
        if (returnTypeElement != null) {
            int returnTypeEndOffset = returnTypeElement.getTextRange().getStartOffset();
            startOffset = Math.min(startOffset, returnTypeEndOffset);
        }

        return startOffset;
    }

    private static int getFieldStartOffset(PsiField psiField) {
        int startOffset = psiField.getTextOffset();

        int modifierStartOffset = getFirstKeywordOffset(psiField);
        if (modifierStartOffset > -1) {
            return Math.min(startOffset, modifierStartOffset);
        }

        PsiTypeElement typeElement = psiField.getTypeElement();
        if (typeElement != null) {
            int typeElementOffset = typeElement.getTextRange().getStartOffset();
            startOffset = Math.min(startOffset, typeElementOffset);
        }

        return startOffset;
    }

    private static int getFirstKeywordOffset(PsiModifierListOwner modifierOwner) {
        PsiModifierList modifierList = modifierOwner.getModifierList();
        if (modifierList != null) {
            PsiElement[] modifiers = modifierList.getChildren();
            for (PsiElement modifier : modifiers) {
                if (modifier instanceof PsiKeyword) {
                    return modifier.getTextRange().getStartOffset();
                }
            }
        }
        return -1;
    }

    private static String getJavaCursorPrefix(Document document, int completionOffset) {
        String fileContents = document.getText();
        int i;
        for (i = completionOffset; i > 0 && !Character.isWhitespace(fileContents.charAt(i - 1)); i--) {
        }
        return fileContents.substring(i, completionOffset);
    }

    /**
     * Given the uri return a {@link PsiFile}. May return null if it can not
     * associate the uri with a Java file or class file.
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
        return resolveTypeRoot(uri, utils);
    }

    public List<CodeAction> getCodeAction(JakartaJavaCodeActionParams params, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<CodeAction>>) () -> {
            return codeActionHandler.codeAction(params, utils);
        });
    }

    private MicroProfileJavaDiagnosticsParams adapt(JakartaDiagnosticsParams params) {
        MicroProfileJavaDiagnosticsParams mpParams = new MicroProfileJavaDiagnosticsParams(params.getUris(),
                new MicroProfileJavaDiagnosticsSettings(Collections.emptyList()));
        mpParams.setDocumentFormat(params.getDocumentFormat());
        return mpParams;
    }
}
