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
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExceptionUtil {

    public static <T> T executeWithExceptionHandling(Supplier<T> action, Consumer<Exception> logger) {
        try {
            return action.get();
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multi-catch to keep backward compatibility
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            // Log the exception using the provided logger
            logger.accept(e);
            return null; // Return null to indicate failure
        }
    }
}