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

import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.terminal.frontend.view.TerminalViewSessionState;
import io.openliberty.tools.intellij.LibertyModule;

/**
 * Determines whether a Liberty dev mode command is currently running in the terminal tab
 * associated with a given {@link LibertyModule}.
 *
 * <p>Strategy (in priority order):
 * <ol>
 *   <li><b>Reworked Terminal available</b> – inspect the {@link TerminalViewSessionState} flow
 *       value from {@link TerminalView#getSessionState()}. {@code Running} means a process is
 *       active; {@code Terminated} / {@code NotStarted} means it is not.</li>
 *   <li><b>No {@link TerminalView} stored</b> – fall back to checking whether
 *       {@code TerminalWidget.getTtyConnector() != null}.</li>
 * </ol>
 */
// TerminalView and related Reworked Terminal APIs are marked @Experimental by JetBrains, but their
// use is explicitly recommended over the Classic Terminal APIs (see https://youtrack.jetbrains.com/issue/IJPL-252504).
@SuppressWarnings("UnstableApiUsage")
public class TerminalCommandUtil {

    private TerminalCommandUtil() {}

    /**
     * Returns {@code true} if no Liberty dev mode command is currently running in the terminal
     * tab associated with the given module.
     */
    public static boolean isCommandNotRunning(LibertyModule module) {
        TerminalView view = module.getTerminalView();
        if (view != null) {
            // Reworked Terminal path
            // Running means a process is active; Terminated / NotStarted means it is not.
            TerminalViewSessionState sessionState = view.getSessionState().getValue();
            return !(sessionState instanceof TerminalViewSessionState.Running);
        }
        // Classic fallback – only reached when no TerminalView is stored.
        return module.getShellWidget() == null
                || module.getShellWidget().getTtyConnector() == null;
    }
}
