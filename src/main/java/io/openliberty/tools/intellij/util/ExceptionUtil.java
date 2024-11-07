/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 *  SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.tools.intellij.util;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;

import java.util.concurrent.CancellationException;
import java.util.function.Function;
import java.util.function.Supplier;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.WorkspaceEdit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is intended for cases where exception handling is repetitive
 */
public class ExceptionUtil {

    /**
     * Executes a supplied action with structured exception handling, allowing
     * the caller to specify a fallback function in case of an exception.
     *
     * @param action  the main operation to execute, represented by a Supplier
     * @param fallback a function to handle exceptions and provide a safe return value
     * @param <T>     the type of result expected from the action
     * @return        the result of the action or the fallback value in case of an exception
     */
    public static <T> T executeWithExceptionHandling(Supplier<T> action, Function<Exception, T> fallback) {
        try {
            return action.get();
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multi-catch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            // Invoke the fallback function with the exception to generate a safe return value.
            return fallback.apply(e);
        }
    }

    /**
     * Executes a workspace edit operation, handling any errors by logging them.
     *
     * @param context    the context required to convert a correction proposal to a workspace edit
     * @param proposal   the proposed correction to be applied
     * @param toResolve  the code action to be resolved with the edit
     * @param logger     a logger instance for logging exceptions if they occur
     * @param logMessage a message to log if an exception is encountered
     */
    public static void executeWithWorkspaceEditHandling(JavaCodeActionResolveContext context, ChangeCorrectionProposal proposal, CodeAction toResolve, Logger logger, String logMessage) {
        executeWithExceptionHandling(
            () -> {
                WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
                toResolve.setEdit(we);
                return true;
            },
            e -> {
                logger.log(Level.WARNING, logMessage, e);
                return false;
            }
        );
    }
}