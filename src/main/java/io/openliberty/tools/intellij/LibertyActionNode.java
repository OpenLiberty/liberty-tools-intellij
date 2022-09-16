/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij;

import javax.swing.tree.DefaultMutableTreeNode;

public class LibertyActionNode extends DefaultMutableTreeNode {
    public String name;

    public LibertyActionNode(String name) {
        super(name);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}