/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation.
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
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.runConfiguration.LibertyRunConfiguration;
import io.openliberty.tools.intellij.runConfiguration.LibertyRunConfigurationType;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
            selectedLibertyConfig = createNewLibertyRunConfig(runManager, libertyModule);
        } else {
            // 1+ run configs found for the given project
            final String[] runConfigNames = toRunConfigNames(libertyModuleSettings);

            //it will display the dialog box with the run configurations with the selected project
            LibertyProjectChooserDialog libertyChooserDiag = new LibertyProjectChooserDialog(project,
                    LocalizedResourceUtil.getMessage("run.config.file.selection.dialog.message", libertyModule.getName()),
                    LocalizedResourceUtil.getMessage("run.config.file.selection.dialog.title"),
                    LibertyPluginIcons.libertyIcon_40, runConfigNames, runConfigNames,
                    runConfigNames[0]);
            libertyChooserDiag.show();

            final int ret = libertyChooserDiag.getSelectedIndex();
            if (ret >= 0 && ret < libertyModuleSettings.size()) {
                selectedLibertyConfig = libertyModuleSettings.get(ret);
            } else {
                // The user pressed cancel on the dialog. No need to show an error message.
                return;
            }
        }
        openRunConfigDialog(selectedLibertyConfig, project);
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

    /**
     * To get the run config names.
     *
     * @param list
     * @return array of run config names.
     */
    protected final String[] toRunConfigNames(@NotNull List<RunnerAndConfigurationSettings> list) {
        final int size = list.size();
        final String[] runConfigNames = new String[size];

        for (int i = 0; i < size; ++i) {
            runConfigNames[i] = list.get(i).getConfiguration().getName();
        }
        return runConfigNames;
    }

    /**
     * Display selectedLibertyConfig
     *
     * @param selectedLibertyConfig
     * @param project
     */
    private void openRunConfigDialog(@NotNull RunnerAndConfigurationSettings selectedLibertyConfig, Project project) {
        // opens run config dialog
        selectedLibertyConfig.setEditBeforeRun(true);
        ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(
                DefaultRunExecutor.getRunExecutorInstance(), selectedLibertyConfig);
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build());
        }
    }
}
