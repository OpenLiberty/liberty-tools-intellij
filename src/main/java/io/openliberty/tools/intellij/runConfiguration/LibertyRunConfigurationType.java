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

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class LibertyRunConfigurationType implements ConfigurationType {
    public static final String ID = "Liberty";

    public static LibertyRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(LibertyRunConfigurationType.class);
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Liberty";
    }

    @Nls
    @Override
    public String getConfigurationTypeDescription() {
        return LocalizedResourceUtil.getMessage("liberty.run.config.title");
    }

    @Override
    public Icon getIcon() {
        return LibertyPluginIcons.libertyIcon;
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new LibertyRunConfigurationFactory(this)};
    }
}