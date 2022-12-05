/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation.
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
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import io.openliberty.tools.intellij.actions.LibertyDevStartAction;
import io.openliberty.tools.intellij.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.util.Collection;

/**
 * Defines a Liberty run & debug configuration. Each configuration is tied to a Liberty module
 */
public class LibertyRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule, LibertyRunConfigurationOptions> {
    protected static Logger LOGGER = Logger.getInstance(LibertyRunConfiguration.class);

    private final LibertyModules libertyModules;
    private LibertyModule libertyModule;

    public LibertyRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(name, getRunConfigurationModule(project), factory);
        this.libertyModules = LibertyModules.getInstance();
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

    // FIXME runInContainer, see https://github.com/OpenLiberty/liberty-tools-intellij/issues/160
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

    /**
     * Runs when users select "Run" or "Debug" on a Liberty  run configuration
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return
     * @throws ExecutionException
     */
    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        try {
            libertyModule = libertyModules.getLibertyProjectFromString(getBuildFile());
        } catch (NullPointerException e) {
            LOGGER.error(String.format("Could not resolve the Liberty module associated with build file: %s", getBuildFile()));
            throw new ExecutionException(e);
        }
        // run the start dev mode action
        AnAction action = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_START_ACTION_ID);
        LibertyDevStartAction libAction = (LibertyDevStartAction) action;
        libAction.setLibertyModule(libertyModule);
        // set custom start params
        if (getParams() != null) {
            libertyModule.setCustomStartParams(getParams());
        } else {
            libertyModule.setCustomStartParams("");
        }
        // FIXME implement runInContainer checkbox from run config see https://github.com/OpenLiberty/liberty-tools-intellij/issues/160
        // libertyModule.setRunInContainer(runInContainer());

        if (executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            libertyModule.setDebugMode(true);
        }
        action.actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(null), ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0));
        // return null because we are not plugging into "Run" tool window in IntelliJ, just terminal and Debug
        return null;
    }

}