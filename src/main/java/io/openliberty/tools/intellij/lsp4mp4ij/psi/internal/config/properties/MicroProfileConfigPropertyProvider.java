/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.config.properties;

import com.intellij.lang.jvm.JvmMember;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.AbstractAnnotationTypeReferencePropertiesProvider;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.SearchContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.apache.commons.lang3.StringUtils;

import static io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.findType;
import static io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getResolvedTypeName;

/**
 * Properties provider to collect MicroProfile properties from the Java fields
 * annotated with "org.eclipse.microprofile.config.inject.ConfigProperty"
 * annotation.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers/MicroProfileConfigPropertyProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers/MicroProfileConfigPropertyProvider.java</a>
 */
public class MicroProfileConfigPropertyProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

	private static final String[] ANNOTATION_NAMES = { MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiModifierListOwner javaElement, PsiAnnotation configPropertyAnnotation,
									 String annotationName, SearchContext context) {
		if (javaElement instanceof PsiField || javaElement instanceof PsiVariable) {
			// Generate the property only class is not annotated with @ConfigProperties
			PsiClass classType = PsiTreeUtil.getParentOfType(javaElement, PsiClass.class);
			boolean hasConfigPropertiesAnnotation = AnnotationUtils.hasAnnotation(classType, MicroProfileConfigConstants.CONFIG_PROPERTIES_ANNOTATION);
			if (!hasConfigPropertiesAnnotation) {
				collectProperty(javaElement, configPropertyAnnotation, null, false, context);
			}
		}
	}

	protected void collectProperty(PsiModifierListOwner javaElement, PsiAnnotation configPropertyAnnotation, String prefix,
								   boolean useFieldNameIfAnnotationIsNotPresent, SearchContext context) {
		String propertyName = getPropertyName(javaElement, configPropertyAnnotation, prefix,
				useFieldNameIfAnnotationIsNotPresent);
		if (propertyName != null && !propertyName.isEmpty()) {
			String defaultValue = configPropertyAnnotation != null
					? AnnotationUtils.getAnnotationMemberValue(configPropertyAnnotation, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE)
					: null;
			collectProperty(javaElement, propertyName, defaultValue, context);
		}
	}

	protected String getPropertyName(PsiElement javaElement, PsiAnnotation configPropertyAnnotation, String prefix,
									 boolean useFieldNameIfAnnotationIsNotPresent) {
		if (configPropertyAnnotation != null) {
			return getPropertyName(AnnotationUtils.getAnnotationMemberValue(configPropertyAnnotation, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME),
					prefix);
		} else if (useFieldNameIfAnnotationIsNotPresent) {
			return getPropertyName(getName(javaElement), prefix);
		}
		return null;
	}

	private static String getName(PsiElement javaElement) {
		if (javaElement instanceof PsiMember) {
			return ((PsiMember) javaElement).getName();
		} else if (javaElement instanceof JvmMember) {
			return ((JvmMember) javaElement).getName();
		}
		return null;
	}

	public static String getPropertyName(String propertyName, String prefix) {
		return StringUtils.isNotEmpty(prefix) ? (prefix + "." + propertyName) : propertyName;
	}

	private void collectProperty(PsiModifierListOwner javaElement, String name, String defaultValue,
								 SearchContext context) {
		Module javaProject = context.getJavaProject();
		String varTypeName = PsiTypeUtils.getResolvedTypeName(javaElement);
		PsiClass varType = PsiTypeUtils.findType(javaProject, varTypeName);
		String type = PsiTypeUtils.getPropertyType(varType, varTypeName);
		String description = null;
		String sourceType = PsiTypeUtils.getSourceType(javaElement);
		String sourceField = null;
		String sourceMethod = null;

		String extensionName = null;

		if (javaElement instanceof PsiField) {
			sourceField = PsiTypeUtils.getSourceField((PsiMember) javaElement);
		} else if (javaElement instanceof PsiVariable) {
			PsiVariable localVariable = (PsiVariable) javaElement;
			PsiMethod method = PsiTreeUtil.getParentOfType(localVariable, PsiMethod.class);
			sourceMethod = PsiTypeUtils.getSourceMethod(method);
		}

		// Enumerations
		PsiClass enclosedType = PsiTypeUtils.getEnclosedType(varType, type, javaElement.getManager());
		super.updateHint(context.getCollector(), enclosedType);

		boolean binary = PsiTypeUtils.isBinary(javaElement);
		super.addItemMetadata(context.getCollector(), name, type, description, sourceType, sourceField, sourceMethod, defaultValue,
				extensionName, binary);
	}

}
