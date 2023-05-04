/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.actions;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.runConfiguration.LibertyRunConfiguration;
import io.openliberty.tools.intellij.runConfiguration.LibertyRunConfigurationType;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens the Liberty run config view for the corresponding Liberty module. Creates a new Liberty run config if one does not exist.
 */
public class LibertyDevCustomStartAction extends LibertyGeneralAction {

    public LibertyDevCustomStartAction() {
        setActionCmd(LocalizedResourceUtil.getMessage("start.liberty.dev.custom.params"));
    }

    @Override
    protected void executeLibertyAction() {
        // open run config
        RunManager runManager = RunManager.getInstance(project);
        List<RunnerAndConfigurationSettings> libertySettings = runManager.getConfigurationSettingsList(LibertyRunConfigurationType.getInstance());
        List<RunnerAndConfigurationSettings> libertyModuleSettings = new ArrayList<>();

        libertySettings.forEach(setting -> {
            // find all Liberty run configs associated with this build file
            LibertyRunConfiguration runConfig = (LibertyRunConfiguration) setting.getConfiguration();
                VirtualFile vBuildFile = VfsUtil.findFile(Paths.get(runConfig.getBuildFile()), true);
                if (vBuildFile != null && vBuildFile.equals(libertyModule.getBuildFile())) {
                    libertyModuleSettings.add(setting);
                }
        });
        RunnerAndConfigurationSettings selectedLibertyConfig;
        if (libertyModuleSettings.isEmpty()) {
            // create new run config
            selectedLibertyConfig = createNewLibertyRunConfig(runManager);
        } else {
            // TODO if 1+ run configs, prompt user to select the one they want see https://github.com/OpenLiberty/liberty-tools-intellij/issues/167
            // 1+ run configs found for the given project
            RunnerAndConfigurationSettings selectedConfig = runManager.getSelectedConfiguration();
            if (libertyModuleSettings.contains(selectedConfig)) {
                // if the selected config is for the Liberty module, use that run config
                selectedLibertyConfig = selectedConfig;
            } else {
                // pick first in list run config in list
                selectedLibertyConfig = libertyModuleSettings.get(0);
            }
        }
        // opens run config dialog
        selectedLibertyConfig.setEditBeforeRun(true);
        ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), selectedLibertyConfig);
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build());
        }
    }

    /**
     * Creates a new run config for the libertyModule selected
     *
     * @param runManager
     * @return RunnerAndConfigurationSettings newly created run config settings
     */
    protected RunnerAndConfigurationSettings createNewLibertyRunConfig(RunManager runManager) {
        RunnerAndConfigurationSettings runConfigSettings = runManager.createConfiguration(runManager.suggestUniqueName(libertyModule.getName(), LibertyRunConfigurationType.getInstance()), LibertyRunConfigurationType.class);
        LibertyRunConfiguration libertyRunConfiguration = (LibertyRunConfiguration) runConfigSettings.getConfiguration();
        // pre-populate build file and name, need to convert build file to NioPath for OS specific paths
        libertyRunConfiguration.setBuildFile(libertyModule.getBuildFile().toNioPath().toString());
        return runConfigSettings;
    }
}
