/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package io.openliberty.tools.intellij.lsp4mp.lsp4ij.server;

import io.openliberty.tools.intellij.lsp4mp.lsp.MicroProfileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public abstract class ProcessStreamConnectionProvider implements StreamConnectionProvider{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessStreamConnectionProvider.class);

    private @Nullable Process process;
    private List<String> commands;
    private @Nullable String workingDir;

    public ProcessStreamConnectionProvider() {
    }

    public ProcessStreamConnectionProvider(List<String> commands) {
        this.commands = commands;
    }

    public ProcessStreamConnectionProvider(List<String> commands, String workingDir) {
        this.commands = commands;
        this.workingDir = workingDir;
    }

    @Override
    public void start() throws IOException {
        if (this.commands == null || this.commands.isEmpty() || this.commands.stream().anyMatch(Objects::isNull)) {
            throw new IOException("Unable to start language server: " + this.toString()); //$NON-NLS-1$
        }

        ProcessBuilder builder = createProcessBuilder();
        Process p = builder.start();
        this.process = p;
        if (!p.isAlive()) {
            throw new IOException("Unable to start language server: " + this.toString()); //$NON-NLS-1$
        } else {
            LOGGER.info("Starting language server: " + this.toString());
        }
    }

    protected ProcessBuilder createProcessBuilder() {
        ProcessBuilder builder = new ProcessBuilder(getCommands());
        if (getWorkingDirectory() != null) {
            builder.directory(new File(getWorkingDirectory()));
        }
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return builder;
    }

    @Override
    public @Nullable InputStream getInputStream() {
        Process p = process;
        return p == null ? null : p.getInputStream();
    }

    @Override
    public @Nullable InputStream getErrorStream() {
        Process p = process;
        return p == null ? null : p.getErrorStream();
    }

    @Override
    public @Nullable OutputStream getOutputStream() {
        Process p = process;
        return p == null ? null : p.getOutputStream();
    }

    @Override
    public void stop() {
        Process p = process;
        if (p != null) {
            p.destroy();
        }
    }

    protected List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    protected @Nullable String getWorkingDirectory() {
        return workingDir;
    }

    public void setWorkingDirectory(String workingDir) {
        this.workingDir = workingDir;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ProcessStreamConnectionProvider)) {
            return false;
        }
        ProcessStreamConnectionProvider other = (ProcessStreamConnectionProvider) obj;
        return Objects.equals(this.getCommands(), other.getCommands())
                && Objects.equals(this.getWorkingDirectory(), other.getWorkingDirectory());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getCommands(), this.getWorkingDirectory());
    }

    @Override
    public String toString() {
        return "ProcessStreamConnectionProvider [commands=" + this.getCommands() + ", workingDir=" //$NON-NLS-1$//$NON-NLS-2$
                + this.getWorkingDirectory() + "]"; //$NON-NLS-1$
    }

}
