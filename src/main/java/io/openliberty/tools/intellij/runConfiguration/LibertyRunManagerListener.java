/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 *  SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.tools.intellij.runConfiguration;

import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

/**
 * Listens to creation/deletion of Liberty run configurations
 */
public class LibertyRunManagerListener implements RunManagerListener {

    protected static Logger LOGGER = Logger.getInstance(LibertyRunManagerListener.class);

    /**
     * When a Liberty run configuration is removed, clear custom run configuration
     * from Liberty module
     *
     * @param settings an IntelliJ run/debug configuration
     */
    @Override
    public void runConfigurationRemoved(@NotNull RunnerAndConfigurationSettings settings) {
        if (settings.getConfiguration() instanceof LibertyRunConfiguration runConfig) {
            LibertyModules libertyModules = LibertyModules.getInstance();
            try {
                VirtualFile vBuildFile = VfsUtil.findFile(Paths.get(runConfig.getBuildFile()), true);
                LibertyModule libertyModule = libertyModules.getLibertyModule(vBuildFile);
                if (libertyModule != null && libertyModule.getCustomRunConfig().equals(runConfig)) {
                    libertyModule.setCustomRunConfig(null);
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Unable to clear custom run configuration for Liberty module: %s. Could not resolve build file.", runConfig.getBuildFile()), e);
            }

        }
    }
}
