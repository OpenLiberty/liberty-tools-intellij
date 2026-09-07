/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.jediterm.terminal.model.TerminalModelListener;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Assisted by IBM Bob
 *
 * Watches a Liberty dev-mode terminal session and updates the
 * {@link LibertyModule.AppState} in response to observable events.
 *
 * <ul>
 *   <li>{@link #watchForRunning} — registers a {@link TerminalModelListener} on the
 *       terminal text buffer. It captures a baseline of the current screen content so
 *       that {@code CWWKF0011I} messages from a previous run are ignored. When the
 *       message appears in <em>new</em> output the module's state is promoted to
 *       {@code RUNNING}.</li>
 *   <li>{@link #watchForStopped} — polls {@link ShellTerminalWidget#hasRunningCommands()}
 *       on a background thread; when the process exits the module's state is set to
 *       {@code STOPPED}.</li>
 * </ul>
 */
public final class LibertyTerminalWatcher {

    /** Liberty "server started" message ID that signals the RUNNING state. */
    private static final String LIBERTY_STARTED_MSG = "CWWKF0011I";

    /** Polling interval when watching for process termination (ms). */
    private static final int STOPPED_POLL_INTERVAL_MS = 1000;

    /** Maximum time to poll for STOPPED before giving up (ms). */
    private static final int STOPPED_POLL_TIMEOUT_MS = 5 * 60 * 1000;

    /** Maximum time to watch for STARTING before timing out or detecting early failure (ms). */
    private static final int STARTING_POLL_TIMEOUT_MS = 10 * 60 * 1000;

    private static final Logger LOGGER = Logger.getInstance(LibertyTerminalWatcher.class);

    private LibertyTerminalWatcher() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Registers a {@link TerminalModelListener} on the terminal's text buffer.
     * A snapshot of the current screen text is captured as a baseline so that any
     * {@code CWWKF0011I} from a previous run is not mistaken for a new start.
     * When the message appears in output written <em>after</em> this call, the module's
     * state is promoted to {@code RUNNING}.
     *
     * <p>Safe to call from any thread.</p>
     *
     * @param widget        The terminal widget running the dev-mode process.
     * @param libertyModule The module whose {@code AppState} to update.
     */
    public static void watchForRunning(ShellTerminalWidget widget, LibertyModule libertyModule) {
        AtomicBoolean completed = new AtomicBoolean(false);

        // Capture the current screen text as a baseline on the EDT (getText() is a Swing call).
        // The listener will only fire when the screen text changes beyond this baseline.
        final String[] baselineHolder = new String[1];
        if (ApplicationManager.getApplication().isDispatchThread()) {
            baselineHolder[0] = safeGetText(widget);
        } else {
            ApplicationManager.getApplication().invokeAndWait(() ->
                baselineHolder[0] = safeGetText(widget)
            );
        }
        final String baseline = baselineHolder[0] != null ? baselineHolder[0] : "";

        // Hold the listener in a one-element array so the lambda can self-reference.
        TerminalModelListener[] holderRef = new TerminalModelListener[1];
        holderRef[0] = () -> {
            if (completed.get()) return;

            // getText() must be called on the EDT.
            ApplicationManager.getApplication().invokeLater(() -> {
                if (completed.get()) return;
                try {
                    String current = safeGetText(widget);
                    if (current == null) return;

                    boolean isStarted = false;
                    // Only count CWWKF0011I that appeared *after* the baseline snapshot.
                    // If the message was already present before we started watching, ignore it.
                    if (current.contains(LIBERTY_STARTED_MSG) && !baseline.contains(LIBERTY_STARTED_MSG)) {
                        isStarted = true;
                    } else if (current.contains(LIBERTY_STARTED_MSG) && !current.equals(baseline)) {
                        // The message was in the baseline too, but the screen has since scrolled /
                        // refreshed — check that the occurrence is at a different position by
                        // comparing the full texts. If new content was added AND CWWKF0011I
                        // appears somewhere after the old baseline length, treat it as a new start.
                        int baseLen = baseline.length();
                        String newPart = current.length() > baseLen ? current.substring(baseLen) : current;
                        if (newPart.contains(LIBERTY_STARTED_MSG)) {
                            isStarted = true;
                        }
                    }

                    if (isStarted && completed.compareAndSet(false, true)) {
                        removeListenerSafe(widget, holderRef[0]);
                        setStateAndRefresh(libertyModule, LibertyModule.AppState.RUNNING);
                    }
                } catch (Exception ex) {
                    LOGGER.warn("LibertyTerminalWatcher: error reading terminal screen text", ex);
                }
            });
        };

        try {
            widget.getTerminalTextBuffer().addModelListener(holderRef[0]);

            // Ensure listener is cleaned up if the terminal widget is closed / disposed
            Disposer.register(widget, () -> {
                if (completed.compareAndSet(false, true)) {
                    removeListenerSafe(widget, holderRef[0]);
                }
            });
        } catch (Exception ex) {
            LOGGER.warn("LibertyTerminalWatcher: could not attach model listener to terminal buffer", ex);
        }

        // Watchdog thread to handle cases where start fails, process exits prematurely, or widget closes
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            long deadline = System.currentTimeMillis() + STARTING_POLL_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline && !completed.get()) {
                try {
                    Thread.sleep(STOPPED_POLL_INTERVAL_MS);
                    if (completed.get()) return;

                    // If the process stopped running before CWWKF0011I was observed, start failed
                    if (!widget.hasRunningCommands()) {
                        if (completed.compareAndSet(false, true)) {
                            removeListenerSafe(widget, holderRef[0]);
                            setStateAndRefresh(libertyModule, LibertyModule.AppState.STOPPED);
                        }
                        return;
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ex) {
                    // Widget disposed or closed
                    if (completed.compareAndSet(false, true)) {
                        removeListenerSafe(widget, holderRef[0]);
                        setStateAndRefresh(libertyModule, LibertyModule.AppState.STOPPED);
                    }
                    return;
                }
            }
        });
    }

    /**
     * Starts a background poller that watches for the terminal process to finish.
     * Once {@link ShellTerminalWidget#hasRunningCommands()} returns {@code false},
     * the module's state is set to {@code STOPPED}.
     *
     * <p>Safe to call from any thread.</p>
     *
     * @param widget        The terminal widget running the dev-mode process.
     * @param libertyModule The module whose {@code AppState} to update.
     */
    public static void watchForStopped(ShellTerminalWidget widget, LibertyModule libertyModule) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            long deadline = System.currentTimeMillis() + STOPPED_POLL_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                try {
                    if (!widget.hasRunningCommands()) {
                        setStateAndRefresh(libertyModule, LibertyModule.AppState.STOPPED);
                        return;
                    }
                    Thread.sleep(STOPPED_POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ex) {
                    // hasRunningCommands() throws IllegalStateException when the widget is
                    // already disposed; treat that as STOPPED.
                    LOGGER.debug("LibertyTerminalWatcher: watchForStopped ended early", ex);
                    setStateAndRefresh(libertyModule, LibertyModule.AppState.STOPPED);
                    return;
                }
            }
            // Timed out — assume stopped.
            LOGGER.warn("LibertyTerminalWatcher: timed out waiting for process to stop for module '"
                    + libertyModule.getName() + "'");
            setStateAndRefresh(libertyModule, LibertyModule.AppState.STOPPED);
        });
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Safely removes the terminal model listener without throwing exceptions. */
    private static void removeListenerSafe(ShellTerminalWidget widget, TerminalModelListener listener) {
        if (listener == null) return;
        try {
            widget.getTerminalTextBuffer().removeModelListener(listener);
        } catch (Exception ignored) {
            // Widget or text buffer might already be disposed
        }
    }

    /** Calls {@link ShellTerminalWidget#getText()} guarding against exceptions. */
    private static String safeGetText(ShellTerminalWidget widget) {
        try {
            return widget.getText();
        } catch (Exception ex) {
            LOGGER.debug("LibertyTerminalWatcher: getText() failed", ex);
            return "";
        }
    }

    /**
     * Updates the module's state, records it in the state cache so it survives a dashboard
     * refresh, and requests a UI refresh on the EDT repainting all visible top-level windows.
     */
    private static void setStateAndRefresh(LibertyModule libertyModule, LibertyModule.AppState newState) {
        libertyModule.setAppState(newState);
        LibertyModules.getInstance().cacheState(libertyModule.getName(), newState);
        ApplicationManager.getApplication().invokeLater(() -> {
            for (java.awt.Window window : java.awt.Window.getWindows()) {
                if (window != null && window.isShowing()) {
                    window.repaint();
                }
            }
        });
    }
}
