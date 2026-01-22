package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di.DependencyInjectionConstants.BUILT_IN_QUALIFIERS;

public class DIUtils extends AbstractDiagnosticsCollector {

    private static final String QUALIFIER_META = "jakarta.inject.Qualifier";
    /**
     * @param annotation
     * @param unit
     * @param type
     * @return
     * @description Method is used to check if the passed annotation is a built in or custom Qualifier
     */
    public static boolean isQualifier(PsiAnnotation annotation, PsiJavaFile unit, PsiClass type) {
        boolean hasBuiltInQualifier = BUILT_IN_QUALIFIERS.stream().anyMatch(qualifier -> {
            return isMatchedJavaElement(type, annotation.getQualifiedName(), qualifier);
        });
        if (!hasBuiltInQualifier) {
            PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
            PsiElement resolved = ref != null ? ref.resolve() : null;
            if (resolved instanceof PsiClass annClass) {
                for (PsiAnnotation metaAnn : annClass.getAnnotations()) {
                    return isMatchedJavaElement(type, metaAnn.getQualifiedName(), QUALIFIER_META);
                }
            }
        }
        return hasBuiltInQualifier;
    }
}
