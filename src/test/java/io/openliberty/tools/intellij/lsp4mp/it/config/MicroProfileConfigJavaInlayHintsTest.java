/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp.it.config;

import com.intellij.openapi.module.Module;
import io.openliberty.tools.intellij.lsp4mp.it.core.BaseMicroProfileTest;
import io.openliberty.tools.intellij.lsp4mp.it.core.MicroProfileForJavaAssert;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.MicroProfileInlayHintTypeSettings;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintSettings;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;

import static io.openliberty.tools.intellij.lsp4mp.it.core.MicroProfileForJavaAssert.*;

@RunWith(JUnit4.class)
public class MicroProfileConfigJavaInlayHintsTest extends BaseMicroProfileTest {

    private static final String PROJECT_DIR =
            "src/test/resources/projects/lsp4mp/maven/config-quickstart";

    private static final String GREETING_RESOURCE =
            "src/main/java/io/openliberty/sample/config/GreetingResource.java";

    @Test
    public void testDefaultValuesAndConverters() throws Exception {
        Module module = createMavenModule(new File(PROJECT_DIR));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaInlayHintParams params = new MicroProfileJavaInlayHintParams();
        params.setUri(getFileUri(GREETING_RESOURCE, module));

        // Both converters and default values enabled
        params.setSettings(createInlayHintSettings(true, true));

        assertInlayHints(params, utils,
                ih(p(25, 11), "BuiltInConverter "),
                ih(p(28, 11), "BuiltInConverter "),
                ih(p(31, 21), "BuiltInConverter "),
                ih(p(34, 8),  "BuiltInConverter "),
                ih(p(36, 46), ", defaultValue=\"PT15M\""),
                ih(p(37, 13), "StaticMethodConverter ")
        );

        // Only converters enabled
        params.setSettings(createInlayHintSettings(true, false));

        assertInlayHints(params, utils,
                ih(p(25, 11), "BuiltInConverter "),
                ih(p(28, 11), "BuiltInConverter "),
                ih(p(31, 21), "BuiltInConverter "),
                ih(p(34, 8),  "BuiltInConverter "),
                ih(p(37, 13), "StaticMethodConverter ")
        );

        // Only default values enabled
        params.setSettings(createInlayHintSettings(false, true));

        assertInlayHints(params, utils,
                ih(p(36, 46), ", defaultValue=\"PT15M\"")
        );
    }

    private static MicroProfileJavaInlayHintSettings createInlayHintSettings(boolean showConverters,
                                                                              boolean showDefaultValues) {
        MicroProfileJavaInlayHintSettings settings =
                new MicroProfileJavaInlayHintSettings(ExecutionMode.SAFE);

        MicroProfileInlayHintTypeSettings converters = new MicroProfileInlayHintTypeSettings();
        converters.setEnabled(showConverters);
        settings.setConverters(converters);

        MicroProfileInlayHintTypeSettings defaultValues = new MicroProfileInlayHintTypeSettings();
        defaultValues.setEnabled(showDefaultValues);
        settings.setDefaultValues(defaultValues);

        return settings;
    }
}
