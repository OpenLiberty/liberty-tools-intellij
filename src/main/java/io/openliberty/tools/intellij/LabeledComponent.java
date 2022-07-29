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

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Creates a panel containing a label and a component. We prefer not to use the
 * {@link com.intellij.openapi.ui.LabeledComponent} because the in that
 * implementation, the beginning of the components (that come after the label)
 * are not vertically aligned together.
 *
 * @author Ehsan Zaery Moghaddam (zaerymoghaddam@gmail.com)
 */
public class LabeledComponent extends JPanel {

 public LabeledComponent(final String labelText,
                         final JComponent component) {
  super(new BorderLayout(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP));
  JBLabel label = new JBLabel();
  label.setText(labelText + ":");

  label.setPreferredSize(new Dimension(220, 20));
  add(label, BorderLayout.WEST);
  add(component, BorderLayout.CENTER);
 }
}
