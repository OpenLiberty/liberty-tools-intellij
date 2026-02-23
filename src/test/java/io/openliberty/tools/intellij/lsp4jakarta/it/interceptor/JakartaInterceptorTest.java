/*******************************************************************************
* Copyright (c) 2026 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.it.interceptor;

import java.io.File;
import java.util.Arrays;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JakartaInterceptorTest extends BaseJakartaTest {

	@Test
	public void invalidInterceptorTest() throws Exception {
		Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
				+ "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidInterceptor.java");
		String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

		JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
		diagnosticsParams.setUris(Arrays.asList(uri));

		// Test diagnostics
		Diagnostic d1 = JakartaForJavaAssert.d(5, 22, 40,
				"The class InvalidInterceptor should not contain the abstract modifier. If it contains the abstract modifier, the class should not be annotated with @Interceptor.",
				DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnAbstractClass");
		Diagnostic d2 = JakartaForJavaAssert.d(5, 22, 40,
				"Missing Public NoArgsConstructor: Class InvalidInterceptor is off Interceptor type, but does not declare a public no-argument constructor.",
				DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnNoArgsConstructor");
		Diagnostic d3 = JakartaForJavaAssert.d(22, 14, 37,
				"Missing Public NoArgsConstructor: Class InnerInvalidInterceptor is off Interceptor type, but does not declare a public no-argument constructor.",
				DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnNoArgsConstructor");

		JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
	}

}
