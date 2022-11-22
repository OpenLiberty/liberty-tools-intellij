/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation.
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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Listens to creation/deletion of Liberty run configurations
 */
public class LibertyRunManagerListener implements RunManagerListener {

    protected static Logger LOGGER = Logger.getInstance(LibertyRunManagerListener.class);

    /**
     * When a Liberty run configuration is removed, clear custom start parameters from Liberty module
     *
     * @param settings
     */
    @Override
    public void runConfigurationRemoved(@NotNull RunnerAndConfigurationSettings settings) {
        if (settings.getConfiguration() instanceof LibertyRunConfiguration) {
            LibertyRunConfiguration runConfig = (LibertyRunConfiguration) settings.getConfiguration();
            LibertyModules libertyModules = LibertyModules.getInstance();
            try {
                VirtualFile vBuildFile = VfsUtil.findFileByURL(new URL(runConfig.getBuildFile()));
                LibertyModule libertyModule = libertyModules.getLibertyModule(vBuildFile);
                if (libertyModule != null && libertyModule.getCustomStartParams().equals(runConfig.getParams())) {
                    libertyModule.clearCustomStartParams();
                }
            } catch (MalformedURLException e) {
                LOGGER.warn(String.format("Unable to clear custom start parameters for Liberty project associated with Liberty run configuration associated with: %s. Could not resolve build file.", runConfig.getBuildFile()), e);
            }

        }
    }
}
