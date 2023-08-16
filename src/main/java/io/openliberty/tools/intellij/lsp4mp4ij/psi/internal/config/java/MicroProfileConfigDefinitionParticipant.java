/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.config.java;

import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.definition.PropertiesDefinitionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;

/**
 *
 * MicroProfile Config Definition to navigate from Java
 * file @ConfigProperty/name to properties, yaml files where the property is
 * declared.
 *
 * @author Angelo ZERR
 *
 * @See https://github.com/eclipse/microprofile-config
 *
 */
public class MicroProfileConfigDefinitionParticipant extends PropertiesDefinitionParticipant {

	public MicroProfileConfigDefinitionParticipant() {
		super(MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION, new String[] {
				MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME
		});
	}
}
