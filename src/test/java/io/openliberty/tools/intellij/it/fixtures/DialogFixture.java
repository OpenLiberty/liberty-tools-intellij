/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.it.fixtures;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.search.locators.Locators;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * Dialog view fixture.
 */
@FixtureName(name = "Dialog")
@DefaultXpath(by = "MyDialog type", xpath = "//div[@class='MyDialog']")
public class DialogFixture extends CommonContainerFixture {

    /**
     * Constructor.
     *
     * @param remoteRobot     The RemoteRobot instance.
     * @param remoteComponent The RemoteComponent instance.
     */
    public DialogFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    /**
     * Returns the Dialog locator with the specified title.
     *
     * @param title The dialog title.
     * @return The Dialog locator with the specified title.
     */
    @NotNull
    public static Locator byTitle(@NotNull String title) {
        return Locators.byXpath("title " + title, "//div[@title='" + title + "' and @class='MyDialog']");
    }

    /**
     * Returns the component fixture associated with the Tree class.
     *
     * @return The component fixture associated with the Tree class.
     */
    public ComponentFixture getTree() {
        return find(ComponentFixture.class, byXpath("//div[@class='Tree']"), Duration.ofSeconds(10));
    }

    /**
     * Returns the JTextFieldFixture object associated with the BorderlessTextField class.
     *
     * @return The JTextFieldFixture object associated with the BorderlessTextField class.
     */
    public JTextFieldFixture getBorderLessTextField() {
        return textField(byXpath("//div[@class='BorderlessTextField']"), Duration.ofSeconds(10));
    }

    /**
     * Returns the JButtonFixture object associated with button containing the specified text.
     *
     * @param text The text associated with the button.
     * @return The JButtonFixture object associated with button containing the specified text.
     */
    public JButtonFixture getButton(String text) {
        return button(byXpath("//div[@text='" + text + "']"), Duration.ofSeconds(5));
    }
}
