/*******************************************************************************
 * Copyright (c) 2020, 2025 Red Hat Inc. and others.
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
package io.openliberty.tools.intellij.lsp4mp.it.core;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintLabelPart;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintParams;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MicroProfileForJavaAssert {

    public static void assertInlayHints(MicroProfileJavaInlayHintParams params,
                                        IPsiUtils utils,
                                        InlayHint... expected) {
        List<InlayHint> actual = PropertiesManagerForJava.getInstance()
                .inlayHint(params, utils, new EmptyProgressIndicator());
        assertInlayHint(actual, expected);
    }

    public static InlayHint ih(Position position, String label) {
        return new InlayHint(position, Either.forLeft(label));
    }

    public static InlayHint ih(Position position, InlayHintLabelPart... parts) {
        return new InlayHint(position, Either.forRight(Arrays.asList(parts)));
    }

    public static void assertInlayHint(List<InlayHint> actual, InlayHint... expected) {
        assertEquals("Inlay hint count mismatch", expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals("position at index " + i, expected[i].getPosition(), actual.get(i).getPosition());
            assertEquals("label at index " + i, expected[i].getLabel(), actual.get(i).getLabel());
        }
    }

    public static Position p(int line, int character) {
        return new Position(line, character);
    }
}
