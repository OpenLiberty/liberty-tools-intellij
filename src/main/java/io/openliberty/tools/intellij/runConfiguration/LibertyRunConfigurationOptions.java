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

import com.intellij.execution.configurations.ModuleBasedConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;
import com.intellij.openapi.diagnostic.Logger;

/**
 * Defines options that can be configured for each Liberty run & debug configuration
 */
public class LibertyRunConfigurationOptions extends ModuleBasedConfigurationOptions {
    protected static Logger LOGGER = Logger.getInstance(LibertyRunConfigurationOptions.class);
    private final StoredProperty<String> paramsProperty = string("").provideDelegate(this, "params");

    private final StoredProperty<String> buildFileProperty = string("").provideDelegate(this, "buildFile");

    private final StoredProperty<Boolean> runInContainerProperty = property(false).provideDelegate(this, "runInContainer");

    public String getParams() {
        return paramsProperty.getValue(this);
    }

    public void setParams(String params) {
        paramsProperty.setValue(this, params);
    }

    public String getBuildFile() {
        return buildFileProperty.getValue(this);
    }

    public void setBuildFile(String buildFile) {
        buildFileProperty.setValue(this, buildFile);
    }

    public Boolean runInContainer() {
        return runInContainerProperty.getValue(this);
    }

    public void setRunInContainer(Boolean runInContainer) {
        runInContainerProperty.setValue(this, runInContainer);
    }
}