/*******************************************************************************
 * Copyright (c) 2020, 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.java.completion;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.completion.IJavaCompletionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.completion.JavaCompletionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class around {@link IJavaCompletionParticipant} participants.
 */
public final class JavaCompletionDefinition extends BaseKeyedLazyInstance<IJavaCompletionParticipant>
        implements IJavaCompletionParticipant {

    public static final ExtensionPointName<JavaCompletionDefinition> EP_NAME = ExtensionPointName.create("open-liberty.intellij.javaCompletionParticipant");

    private static final Logger LOGGER = Logger.getLogger(JavaCompletionDefinition.class.getName());
    private static final String GROUP_ATTR = "group";
    private static final String IMPLEMENTATION_CLASS_ATTR = "implementationClass";

    @Attribute(GROUP_ATTR)
    private String group;

    @Attribute(IMPLEMENTATION_CLASS_ATTR)
    public String implementationClass;

    @Override
    public boolean isAdaptedForCompletion(JavaCompletionContext context) {
        try {
            return getInstance().isAdaptedForCompletion(context);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while calling isAdaptedForCompletion", e);
            return false;
        }
    }

    @Override
    public List<? extends CompletionItem> collectCompletionItems(JavaCompletionContext context) {
        try {
            return getInstance().collectCompletionItems(context);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while calling collectCompletionItems", e);
            return Collections.emptyList();
        }
    }

    public @Nullable String getGroup() {
        return group;
    }

    @Override
    protected @Nullable String getImplementationClassName() {
        return implementationClass;
    }
}
