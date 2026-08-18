/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.liberty.lsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;

/**
 * Client for Liberty language server
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusLanguageClient.java
 */
public class LibertyConfigLanguageClient extends LanguageClientImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyConfigLanguageClient.class);

    public LibertyConfigLanguageClient(Project project) {
        super(project);
    }

}
