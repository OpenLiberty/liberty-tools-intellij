/*******************************************************************************
 * Copyright (c) 2020, 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.completion;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.java.completion.JavaCompletionDefinition;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4mp.commons.JavaCursorContextKind;
import org.eclipse.lsp4mp.commons.JavaCursorContextResult;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class CompletionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompletionHandler.class);

    private final String group;

    public CompletionHandler(String group) {
        this.group = group;
    }

    /**
     * Returns the CompletionItems given the completion item params
     *
     * @param params  the completion item params
     * @param utils   the IJDTUtils
     * @return the CompletionItems for the given the completion item params
     */
    public CompletionList completion(MicroProfileJavaCompletionParams params, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<CompletionList>) () -> {
            try {
                String uri = params.getUri();
                PsiFile typeRoot = resolveTypeRoot(uri, utils);
                if (typeRoot == null) {
                    return null;
                }

                Module module = utils.getModule(uri);
                if (module == null) {
                    return null;
                }

                Position completionPosition = params.getPosition();
                int completionOffset = utils.toOffset(typeRoot, completionPosition.getLine(),
                        completionPosition.getCharacter());

                List<CompletionItem> completionItems = new ArrayList<>();
                JavaCompletionContext completionContext = new JavaCompletionContext(uri, typeRoot, utils, module, completionOffset);

                List<JavaCompletionDefinition> completions = JavaCompletionDefinition.EP_NAME.getExtensionList().stream()
                        .filter(definition -> group.equals(definition.getGroup()))
                        .filter(completion -> completion.isAdaptedForCompletion(completionContext))
                        .collect(Collectors.toList());

                if (completions.isEmpty()) {
                    return null;
                }

                completions.forEach(completion -> {
                    List<? extends CompletionItem> collectedCompletionItems = completion.collectCompletionItems(completionContext);
                    if (collectedCompletionItems != null) {
                        completionItems.addAll(collectedCompletionItems);
                    }
                });

                CompletionList completionList = new CompletionList();
                completionList.setItems(completionItems);
                return completionList;
            } catch (IOException e) {
                LOGGER.warn(e.getLocalizedMessage(), e);
                return null;
            }
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

        JavaCursorContextKind kind = getJavaCursorContextKind((PsiJavaFile) typeRoot, completionOffset);
        String prefix = getJavaCursorPrefix(document, completionOffset);

        return new JavaCursorContextResult(kind, prefix);
    }

    private static @NotNull JavaCursorContextKind getJavaCursorContextKind(PsiJavaFile javaFile, int completionOffset) {
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
            @Nullable PsiAnnotationOwner annotationOwner = psiAnnotation.getOwner();
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

    @NotNull
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

    private static @NotNull String getJavaCursorPrefix(@NotNull Document document, int completionOffset) {
        String fileContents = document.getText();
        int i;
        for (i = completionOffset; i > 0 && !Character.isWhitespace(fileContents.charAt(i - 1)); i--) {
        }
        return fileContents.substring(i, completionOffset);
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

    private static int getClassStartOffset(PsiClass psiClass) {
        int startOffset = psiClass.getTextOffset();

        int modifierStartOffset = getFirstKeywordOffset(psiClass);
        if (modifierStartOffset > -1) {
            return Math.min(startOffset, modifierStartOffset);
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

    // REVISIT: Make this a public method on a common utility class?
    private static PsiFile resolveTypeRoot(String uri, IPsiUtils utils) {
        return utils.resolveCompilationUnit(uri);
    }
}
