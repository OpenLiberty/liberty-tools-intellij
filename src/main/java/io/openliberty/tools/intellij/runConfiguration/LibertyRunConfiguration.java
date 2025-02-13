/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.runConfiguration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import io.openliberty.tools.intellij.util.Constants;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Defines a Liberty run & debug configuration. Each configuration is tied to a Liberty module
 */
public class LibertyRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule, LibertyRunConfigurationOptions> {
    protected static Logger LOGGER = Logger.getInstance(LibertyRunConfiguration.class);

    private final LibertyModules libertyModules;
    @NonNls
    private static final String RUN_IN_CONTAINER_TAG = "RUN_IN_CONTAINER";

    public LibertyRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(name, getRunConfigurationModule(project), factory);
        // Find Liberty modules here to populate config field called "build file" and avoid NPE
        this.libertyModules = LibertyModules.getInstance().rescanLibertyModules(project);
    }

    @NotNull
    private static RunConfigurationModule getRunConfigurationModule(Project project) {
        RunConfigurationModule module = new RunConfigurationModule(project);
        // TODO set module to first valid Liberty module?
        module.setModuleToAnyFirstIfNotSpecified();
        return module;
    }

    public Module getModule() {
        return getConfigurationModule().getModule();
    }

    public void setModule(Module module) {
        getConfigurationModule().setModule(module);
    }

    public String getParams() {
        return getOptions().getParams();
    }

    public void setParams(String params) {
        getOptions().setParams(params);
    }

    public String getBuildFile() {
        return getOptions().getBuildFile();
    }

    public void setBuildFile(String buildFile) {
        getOptions().setBuildFile(buildFile);
    }

    public Boolean runInContainer() {
        return getOptions().runInContainer();
    }

    public void setRunInContainer(Boolean runInContainer) {
        getOptions().setRunInContainer(runInContainer);
    }

    @Override
    public Collection<Module> getValidModules() {
        // TODO return only valid Liberty modules?
        return getAllModules();
    }

    @NotNull
    @Override
    protected LibertyRunConfigurationOptions getOptions() {
        return (LibertyRunConfigurationOptions) super.getOptions();
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new LibertyRunSettingsEditor(getProject());
    }

    @Override
    public @Nullable String suggestedName() {
        if (!getName().isEmpty()) {
            // getName() is @notnull
            // "Suggest" current name in case the user typed a name
            return getName();
        } else if (getModule() != null && !getModule().getName().isEmpty()) {
            // getModule().getName() is @notnull
            return getModule().getName();
        }
        return super.suggestedName();
    }

    /**
     * Runs when users select "Run" or "Debug" on a Liberty run configuration
     *
     * @param executor    the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return
     * @throws ExecutionException
     */
    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        LibertyModule libertyModule;
        try {
            libertyModule = libertyModules.getLibertyProjectFromString(getBuildFile());
            libertyModule.setCustomRunConfig(this);
            libertyModule.setUseCustom(true);
            // Previous liberty action may have forced the edit dialog to appear, disable now
            var config = environment.getRunnerAndConfigurationSettings();
            if (config != null) {
                config.setEditBeforeRun(false);
            }
        } catch (NullPointerException e) {
            LOGGER.error(String.format("Could not resolve the Liberty module associated with build file: %s", getBuildFile()));
            throw new ExecutionException(e);
        }
        // run the start dev mode action which also handles runInContainer.
        AnAction action = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_START_ACTION_ID);

        if (executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            libertyModule.setDebugMode(true);
        }

        // Required configuration event data.
        DataContext dataCtx = dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return libertyModule.getProject();
            }
            if (Constants.LIBERTY_BUILD_FILE_DATAKEY.getName().equals(dataId)) {
                return libertyModule.getBuildFile();
            }
            return null;
        };

        AnActionEvent event = new AnActionEvent(null, dataCtx, ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0);
        action.actionPerformed(event);

        // return null because we are not plugging into "Run" tool window in IntelliJ, just terminal and Debug
        return null;
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        JDOMExternalizerUtil.writeField(element, RUN_IN_CONTAINER_TAG, String.valueOf(getOptions().runInContainer()));
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        getOptions().setRunInContainer( Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, RUN_IN_CONTAINER_TAG, Boolean.FALSE.toString())));
    }
}