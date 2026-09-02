package io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.config.java;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiField;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.inlayhint.JavaASTInlayHint;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.inlayhint.JavaInlayHintsContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintSettings;
import org.jetbrains.annotations.NotNull;

import static io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValueExpression;

/**
 * Show converters and default value as inlay hint in Java file for fields which
 * are annotated with ConfigProperty annotation.
 */
public class MicroProfileConfigASTInlayHint extends JavaASTInlayHint {

    @Override
    public void visitAnnotation(@NotNull PsiAnnotation annotation) {
        if (AnnotationUtils.isMatchAnnotation(annotation, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION)) {
            PsiField parent = PsiTreeUtil.getParentOfType(annotation, PsiField.class);
            if (parent != null) {
                JavaInlayHintsContext context = getContext();
                MicroProfileJavaInlayHintSettings settings = context.getSettings();
                if (settings.getDefaultValues().isEnabled()) {
                    generateDefaultValueInlayHint(annotation, context);
                }
                if (settings.getConverters().isEnabled()) {
                    generateConverterInlayHint(parent, context);
                }
            }
        }
    }

    private static void generateConverterInlayHint(PsiField fieldDeclaration,
                                                   JavaInlayHintsContext context) {
        context.addConverterInlayHint(fieldDeclaration.getType(), fieldDeclaration);
    }

    private static void generateDefaultValueInlayHint(PsiAnnotation annotation, JavaInlayHintsContext context) {
        PsiAnnotationMemberValue nameExpr = getAnnotationMemberValueExpression(annotation, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME);
        if (nameExpr != null) {
            PsiAnnotationMemberValue defaultValueExpr = AnnotationUtils.getAnnotationMemberValueExpression(annotation,
                    MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
            if (defaultValueExpr == null) {
                String propertyKey = PsiTypeUtils.extractStringValue(nameExpr);
                if (propertyKey != null) {
                    String propertyValue = context.getMicroProfileProject().getProperty(propertyKey);
                    if (StringUtils.isNotBlank(propertyValue)) {
                        context.addInlayHint(", defaultValue=\"" + propertyValue + "\"",
                                nameExpr.getTextOffset() + nameExpr.getTextLength());
                    }
                }
            }
        }
    }

}