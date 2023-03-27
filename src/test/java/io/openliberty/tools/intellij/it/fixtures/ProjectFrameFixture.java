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
import com.intellij.remoterobot.utils.RepeatUtilsKt;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * Project IDE frame.
 */
@DefaultXpath(by = "IdeFrameImpl type", xpath = "//div[@class='IdeFrameImpl']")
@FixtureName(name = "Project Frame")
public class ProjectFrameFixture extends CommonContainerFixture {

    /**
     * Constructor.
     *
     * @param remoteRobot     The RemoteRobot instance.
     * @param remoteComponent the RemoteComponent instance.
     */
    public ProjectFrameFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }


    /**
     * Returns the ComponentFixture object associated with the input locator.
     *
     * @param locator The custom locator.
     * @return The ComponentFixture object associated with the input locator.
     */
    public ComponentFixture getActionButton(Locator locator) {
        return find(ComponentFixture.class, locator, Duration.ofSeconds(10));
    }

    /**
     * Returns the ComponentFixture object associated with the ActionMenu class.
     *
     * @param xpathVars The Locator custom variables: text
     * @return The ComponentFixture object associated with the ActionMenu class.
     */
    public ComponentFixture getActionMenu(String... xpathVars) {
        String text = xpathVars[0];
        return find(ComponentFixture.class, byXpath("//div[@class='ActionMenu' and @text='" + text + "']"), Duration.ofSeconds(10));
    }

    /**
     * Returns the ComponentFixture object associated with the ActionMenuItem class.
     *
     * @param xpathVars The Locator custom variables: text
     * @return The ComponentFixture object associated with the ActionMenuItem class.
     */
    public ComponentFixture getActionMenuItem(String... xpathVars) {
        String text = xpathVars[0];
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for menu items containing the " + text + " text",
                "Menu items containing the " + text + " text were not found",
                () -> !findAll(ComponentFixture.class,
                        byXpath("//div[@class='ActionMenuItem' and @text='" + text + "']")).isEmpty());
        List<ComponentFixture> list = findAll(ComponentFixture.class, byXpath("//div[@class='ActionMenuItem' and @text='" + text + "']"));
        return list.get(0);
    }
    
    /**
     * Returns the ComponentFixture object associated with the ActionMenu class.
     *
     * @param xpathVars The Locator custom variables: text
     * @return The ComponentFixture object associated with the ActionMenu class.
     */
    public ComponentFixture getChildActionMenu(String... xpathVars) {
        String parentText = xpathVars[0];
        String childText = xpathVars[1];
        return find(ComponentFixture.class, byXpath("//div[@class='ActionMenu' and @text='" + parentText + "']//div[@class='ActionMenu' and @text='" + childText + "']"), Duration.ofSeconds(10));
    }

    /**
     * Returns the ComponentFixture object associated with the ActionMenuItem class.
     *
     * @param xpathVars The Locator custom variables: text
     * @return The ComponentFixture object associated with the ActionMenuItem class.
     */
    public ComponentFixture getChildActionMenuItem(String... xpathVars) {
        String parentText = xpathVars[0];
        String childText = xpathVars[1];
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for child menu items containing the " + childText + " text",
                "Child menu items containing the " + childText + " text were not found",
                () -> !findAll(ComponentFixture.class,
                        byXpath("//div[@class='ActionMenu' and @text='" + parentText + "']//div[@class='ActionMenuItem' and @text='" + childText + "']")).isEmpty());
        List<ComponentFixture> list = findAll(ComponentFixture.class, byXpath("//div[@class='ActionMenu' and @text='" + parentText + "']//div[@class='ActionMenuItem' and @text='" + childText + "']"));
        return list.get(0);
    }

    /**
     * Returns the ComponentFixture object associated with the BaseLabel class.
     *
     * @param xpathVars The Locator custom variables: text, waitTime(secs)
     * @return The ComponentFixture object associated with the BaseLabel class.
     */
    public ComponentFixture getBaseLabel(String... xpathVars) {
        String text = xpathVars[0];
        String waitTime = xpathVars[1];
        return find(ComponentFixture.class,
                byXpath("//div[@class='BaseLabel' and @text='" + text + "']"),
                Duration.ofSeconds(Integer.valueOf(waitTime)));
    }

    /**
     * Returns the ComponentFixture object associated with the ContentComboLabel class.
     *
     * @param xpathVars The Locator custom variables: text, waitTime(secs)
     * @return The ComponentFixture object associated with the ContentComboLabel class.
     */
    public ComponentFixture getContentComboLabel(String... xpathVars) {
        String text = xpathVars[0];
        String waitTime = xpathVars[1];

        return find(ComponentFixture.class,
                byXpath("//div[@class='ContentComboLabel' and @text='" + text + "']"),
                Duration.ofSeconds(Integer.valueOf(waitTime)));
    }

    /**
     * Returns the ComponentFixture object associated with the ProjectViewTree class.
     *
     * @param xpathVars The Locator custom variables: text
     * @return The ComponentFixture object associated with the ProjectViewTree class.
     */
    public ComponentFixture getProjectViewTree(String... xpathVars) {
        String visibleText = xpathVars[0];

        return find(ComponentFixture.class,
                byXpath("//div[@class='ProjectViewTree' and contains(@visible_text, '" + visibleText + "')]"),
                Duration.ofMinutes(1));
    }

    /**
     * Returns the JTreeFixture object associated with the ProjectViewTree class.
     *
     * @param xpathVars The Locator custom variables: text
     * @return The ComponentFixture object associated with the ProjectViewTree class.
     */
    public JTreeFixture getProjectViewJTree(String... xpathVars) {
        String visibleText = xpathVars[0];
        //return find(JTreeFixture.class, JTreeFixture.Companion.byType(), Duration.ofSeconds(10));
        return find(JTreeFixture.class,
                byXpath("//div[@class='ProjectViewTree' and contains(@visible_text, '" + visibleText + "')]"),
                Duration.ofMinutes(1));
    }

    /**
     * Returns the ComponentFixture object associated with the StripeButton class.
     *
     * @param xpathVars The Locator custom variables: text
     * @return The ComponentFixture object associated with the StripeButton class.
     */
    public ComponentFixture getStripeButton(String... xpathVars) {
        String text = xpathVars[0];

        return find(ComponentFixture.class,
                byXpath("//div[@class='StripeButton' and @text='" + text + "']"),
                Duration.ofSeconds(10));
    }

    /**
     * Returns the ComponentFixture object associated with the STETabLabel class.
     *
     * @param xpathVars The Locator custom variables: text, waitTime(secs)
     * @return The ComponentFixture object associated with the STETabLabel class.
     */
    public ComponentFixture getSTELabel(String... xpathVars) {
        String text = xpathVars[0];
        return find(ComponentFixture.class,
                byXpath("//div[@class='SETabLabel' and @text='" + text + "']"),
                Duration.ofSeconds(5));
    }

    /**
     * Returns the ComponentFixture object associated with the Tree class.
     *
     * @param xpathVars The Locator custom variables: name, visibleText, waitTime(mins)
     * @return The ComponentFixture object associated with the Tree class.
     */
    public ComponentFixture getTree(String... xpathVars) {
        String name = xpathVars[0];
        String visibleText = xpathVars[1];
        String waitTime = xpathVars[2];

        return find(ComponentFixture.class,
                byXpath("//div[@class='Tree' and @name='" + name + "' and contains(@visible_text, '" + visibleText + "')]"),
                Duration.ofMinutes(Integer.valueOf(waitTime)));
    }

    /**
     * Return the ComponentFixture object associated with the InplaceButton class.
     *
     * @param xpathVars The Locator custom variables: name, waitTime(mins)
     * @return The ComponentFixture object associated with the InplaceButton class.
     */
    public ComponentFixture getInplaceButton(String... xpathVars) {
        String name = xpathVars[0];
        String waitTime = xpathVars[1];
        Locator locator = byXpath("//div[@accessiblename='" + name + "' and @class='SingleHeightLabel']//div[@class='InplaceButton']");
        return find(ComponentFixture.class, locator, Duration.ofSeconds(Integer.valueOf(waitTime)));
    }

    /**
     * Returns the ComponentFixture object associated with the Editor class.
     *
     * @param xpathVars The Locator custom variables.
     * @return The ComponentFixture object associated with the ProjectViewTree class.
     */
    public ComponentFixture getEditorPane(String... xpathVars) {
        String visibleText = xpathVars[0];
        return find(ComponentFixture.class, byXpath("//div[@class='EditorComponentImpl' and contains(@visible_text, '" + visibleText + "')]"), Duration.ofMinutes(1));
    }

    /**
     * Returns the ContainerFixture object associated with the hover text DocumentationHint popup.
     *
     * @return The ContainerFixture object associated with the current popup.
     */
    public ContainerFixture getDocumentationHintPopup() {
        return find(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']"), Duration.ofSeconds(20));
    }
}
