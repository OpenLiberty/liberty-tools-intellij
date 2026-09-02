/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.util.terminal;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.terminal.frontend.view.TerminalViewSessionState;
import io.openliberty.tools.intellij.LibertyModule;
import kotlinx.coroutines.Deferred;
import org.jetbrains.plugins.terminal.view.shellIntegration.TerminalOutputStatus;
import org.jetbrains.plugins.terminal.view.shellIntegration.TerminalShellIntegration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Determines whether a Liberty dev mode command is currently running in the terminal tab
 * associated with a given {@link LibertyModule}.
 *
 * <p>Strategy (in priority order):
 * <ol>
 *   <li><b>Shell integration available</b> – read
 *       {@link TerminalShellIntegration#getOutputStatus() getOutputStatus().getValue()} from the
 *       non-blocking {@code StateFlow}. A command is running when the status is
 *       {@link TerminalOutputStatus.ExecutingCommand}.</li>
 *   <li><b>Shell integration not yet available</b> – inspect the
 *       {@link TerminalViewSessionState} flow value. {@code Running} means a process is active;
 *       {@code Terminated} / {@code NotStarted} means it is not.</li>
 *   <li><b>No {@link TerminalView} stored</b> – fall back to checking whether
 *       {@code TerminalWidget.getTtyConnector() != null}.</li>
 * </ol>
 */
public class TerminalCommandUtil {

    private static final Logger LOGGER = Logger.getInstance(TerminalCommandUtil.class);

    private TerminalCommandUtil() {}

    /**
     * Returns {@code true} if a command (Liberty dev mode) is currently running in the terminal
     * tab associated with the given module.
     */
    public static boolean isCommandRunning(LibertyModule module) {
        TerminalView view = module.getTerminalView();
        if (view != null) {
            // Path 1: try shell integration – non-blocking StateFlow read.
            Boolean shellResult = isCommandRunningViaShellIntegration(view);
            if (shellResult != null) {
                return shellResult;
            }
            // Path 2: session state – available without shell integration.
            TerminalViewSessionState sessionState = view.getSessionState().getValue();
            if (sessionState instanceof TerminalViewSessionState.Terminated) return false;
            if (sessionState instanceof TerminalViewSessionState.NotStarted) return false;
            // TerminalViewSessionState.Running – a process is active.
            return true;
        }
        // Path 3: classic fallback.
        return module.getShellWidget() != null
                && module.getShellWidget().getTtyConnector() != null;
    }

    /**
     * Reads {@code TerminalShellIntegration.getOutputStatus().getValue()} without blocking.
     * Returns {@code null} when shell integration is not yet initialised (Deferred not completed).
     */
    private static Boolean isCommandRunningViaShellIntegration(TerminalView view) {
        try {
            Deferred<TerminalShellIntegration> deferred = view.getShellIntegrationDeferred();
            if (!deferred.isCompleted()) return null;
            TerminalShellIntegration integration = deferred.getCompleted();
            if (integration == null) return null;
            TerminalOutputStatus status = integration.getOutputStatus().getValue();
            return status instanceof TerminalOutputStatus.ExecutingCommand;
        } catch (IllegalStateException e) {
            // getCompleted() throws if the Deferred completed exceptionally.
            return null;
        }
    }
}
