/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 *  SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.tools.intellij.util;

/**
 * Used for creating custom Liberty exceptions
 */
public class LibertyException extends Exception {
    private String translatedMessage;

    /**
     * Constructor that accepts default message and translated message
     * @param message exception message to use on logs
     * @param translatedMessage exception message to display for end-user
     */
    public LibertyException(String message, String translatedMessage) {
        super(message);
        this.translatedMessage = translatedMessage;
    }

    /**
     * Default constructor
     * @param message exception message to use on logs
     */
    public LibertyException(String message) {
        this(message, message); // in this case, there is no difference between messages
    }

    /**
     * Gets the translated message
     * @return translated message to display to end-user
     */
    public String getTranslatedMessage() {
        return translatedMessage;
    }
}
