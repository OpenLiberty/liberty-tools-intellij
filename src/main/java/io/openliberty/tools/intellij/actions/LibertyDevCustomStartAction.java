/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation.
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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.runConfiguration.LibertyRunConfiguration;
import io.openliberty.tools.intellij.runConfiguration.LibertyRunConfigurationType;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;

import java.nio.file.Paths;
import java.util.*;

/**
 * Opens the Liberty run config view for the corresponding Liberty module. Creates a new Liberty run config if one does not exist.
 */
public class LibertyDevCustomStartAction extends LibertyGeneralAction {

    /**
     * Returns the name of the action command being processed.
     *
     * @return The name of the action command being processed.
     */
    protected String getActionCommandName() {
        return LocalizedResourceUtil.getMessage("start.liberty.dev.custom.params");
    }

    @Override
    protected void executeLibertyAction(LibertyModule libertyModule) {
        Project project = libertyModule.getProject();

        // determine which run config is selected in the UI
        RunManager runManager = RunManager.getInstance(project);
        List<RunnerAndConfigurationSettings> libertySettings = runManager.getConfigurationSettingsList(LibertyRunConfigurationType.getInstance());
        Map<LibertyRunConfiguration, RunnerAndConfigurationSettings> libertyModuleSettings = new HashMap<>();

        libertySettings.forEach(setting -> {
            // find all Liberty run configs associated with this liberty module
            LibertyRunConfiguration runConfig = (LibertyRunConfiguration) setting.getConfiguration();
            VirtualFile vBuildFile = VfsUtil.findFile(Paths.get(runConfig.getBuildFile()), true);
            if (vBuildFile != null && vBuildFile.equals(libertyModule.getBuildFile())) {
                libertyModuleSettings.put(runConfig, setting);
            }
        });
        // Select a run configuration based on the following priority
        // 1) a config is selected in the IntelliJ Run / Debug Configurations combobox
        // 2) a config was previously used
        // 3) this is the first time you used this action or you deleted the config you used last time so
        //    we select another config created for the current module
        // 4) show a dialog to create a new run/debug configuration
        RunnerAndConfigurationSettings selectedLibertySettings = null;
        if (libertyModuleSettings.isEmpty()) {
            // create new run config
            selectedLibertySettings = createNewLibertyRunConfig(runManager, libertyModule);
        } else {
            RunnerAndConfigurationSettings selectedSettings = runManager.getSelectedConfiguration();
            if (libertyModuleSettings.containsValue(selectedSettings)) {
                // if the selected config is for the Liberty module, use that run config
                selectedLibertySettings = selectedSettings;
            } else if (libertyModule.getCustomRunConfig() != null) {
                // if the custom run config is set then we expect to find it in the settings retrieved from RunManager
                selectedLibertySettings = libertyModuleSettings.get(libertyModule.getCustomRunConfig());
            } else {
                // pick first run config settings in list
                selectedLibertySettings = libertyModuleSettings.values().iterator().next();
            }
        }
        // set up the module for use in LibertyDevStartAction after Run button is pressed in config dialog
        libertyModule.setCustomRunConfig((LibertyRunConfiguration)selectedLibertySettings.getConfiguration());
        libertyModule.setUseCustom(true);

        // opens run config dialog
        selectedLibertySettings.setEditBeforeRun(true);
        ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), selectedLibertySettings);
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
    protected RunnerAndConfigurationSettings createNewLibertyRunConfig(RunManager runManager, LibertyModule libertyModule) {
        RunnerAndConfigurationSettings runConfigSettings = runManager.createConfiguration(runManager.suggestUniqueName(libertyModule.getName(), LibertyRunConfigurationType.getInstance()), LibertyRunConfigurationType.class);
        LibertyRunConfiguration libertyRunConfiguration = (LibertyRunConfiguration) runConfigSettings.getConfiguration();
        // pre-populate build file and name, need to convert build file to NioPath for OS specific paths
        libertyRunConfiguration.setBuildFile(libertyModule.getBuildFile().toNioPath().toString());
        return runConfigSettings;
    }
}
