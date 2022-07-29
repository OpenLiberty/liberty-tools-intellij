/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.intellij;

import com.intellij.ui.ColoredListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Given the version number as e.g. MP23, MP30 and renders it as e.g. 2.3 or 3.0
 *
 * @author Ehsan Zaery Moghaddam (zaerymoghaddam@gmail.com)
 */
public class MPVersionComboBoxRenderer extends ColoredListCellRenderer<String> {

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends String> list, String mpVersion, int index, boolean selected, boolean hasFocus) {
        String version = mpVersion.substring(2, 3) + "." + mpVersion.substring(3, 4);
        append(version);
    }
}