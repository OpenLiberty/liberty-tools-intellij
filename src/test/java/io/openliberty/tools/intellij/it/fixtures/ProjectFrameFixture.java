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
import com.intellij.ui.HyperlinkLabel;
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
     * @param vars The custom variables to use: xPath and waitTime(seconds)
     * @return The ComponentFixture object associated with the input locator.
     */
    public ComponentFixture getActionButton(String... vars) {
        String xPath = vars[0];
        int waitTime = Integer.parseInt(vars[1]);

        Locator locator = byXpath(xPath);
        return find(ComponentFixture.class, locator, Duration.ofSeconds(waitTime));
    }

    /**
     * Returns the ComponentFixture object associated with the ActionMenu class.
     *
     * @param xpathVars The Locator custom variables: text, waitTime(secs)
     * @return The ComponentFixture object associated with the ActionMenu class.
     */
    public ComponentFixture getActionMenu(String... xpathVars) {
        String text = xpathVars[0];
        String waitTime = xpathVars[1];
        return find(ComponentFixture.class,
                byXpath("//div[@class='ActionMenu' and @text='" + text + "']"),
                Duration.ofSeconds(Integer.parseInt(waitTime)));
    }

    /**
     * Returns the ComponentFixture object associated with the ActionMenuItem class.
     *
     * @param xpathVars The Locator custom variables: text
     * @return The ComponentFixture object associated with the ActionMenuItem class.
     */
    public ComponentFixture getActionMenuItem(String... xpathVars) {
        String text = xpathVars[0];
        RepeatUtilsKt.waitFor(Duration.ofSeconds(16),
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
     * @param xpathVars The Locator custom variables: text, waitTime(seconds)
     * @return The ComponentFixture object associated with the BaseLabel class.
     */
    public ComponentFixture getBaseLabel(String... xpathVars) {
        String text = xpathVars[0];
        String waitTime = xpathVars[1];
        return find(ComponentFixture.class,
                byXpath("//div[@class='BaseLabel' and @text='" + text + "']"),
                Duration.ofSeconds(Integer.parseInt(waitTime)));
    }

    /**
     * Returns the ComponentFixture object associated with the ContentComboLabel class.
     *
     * @param xpathVars The Locator custom variables: text, waitTime(seconds)
     * @return The ComponentFixture object associated with the ContentComboLabel class.
     */
    public ComponentFixture getContentComboLabel(String... xpathVars) {
        String text = xpathVars[0];
        String waitTime = xpathVars[1];

        return find(ComponentFixture.class,
                byXpath("//div[@class='ContentComboLabel' and @text='" + text + "']"),
                Duration.ofSeconds(Integer.parseInt(waitTime)));
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
     * @param xpathVars The Locator custom variables: text, waitTime(secs)
     * @return The ComponentFixture object associated with the StripeButton class.
     */
    public ComponentFixture getStripeButton(String... xpathVars) {
        String text = xpathVars[0];
        String waitTime = xpathVars[1];
        return find(ComponentFixture.class,
                byXpath("//div[@class='StripeButton' and @text='" + text + "']"),
                Duration.ofSeconds(Integer.parseInt(waitTime)));
    }

    /**
     * Returns the ComponentFixture object associated with the SETabLabel class.
     *
     * @param xpathVars The Locator custom variables: text, waitTime(secs)
     * @return The ComponentFixture object associated with the SETabLabel class.
     */
    public ComponentFixture getSETabLabel(String... xpathVars) {
        String text = xpathVars[0];
        return find(ComponentFixture.class,
                byXpath("//div[@class='SETabLabel' and @text='" + text + "']"),
                Duration.ofSeconds(10));
    }

    /**
     * Returns the ComponentFixture object associated with the Tree class.
     *
     * @param xpathVars The Locator custom variables: name, visibleText, waitTime(seconds)
     * @return The ComponentFixture object associated with the Tree class.
     */
    public ComponentFixture getTree(String... xpathVars) {
        String name = xpathVars[0];
        String visibleText = xpathVars[1];
        String waitTime = xpathVars[2];

        return find(ComponentFixture.class,
                byXpath("//div[@class='Tree' and @name='" + name + "' and contains(@visible_text, '" + visibleText + "')]"),
                Duration.ofSeconds(Integer.parseInt(waitTime)));
    }

    /**
     * Return the ComponentFixture object associated with the InplaceButton class.
     *
     * @param xpathVars The Locator custom variables: name, waitTime(seconds)
     * @return The ComponentFixture object associated with the InplaceButton class.
     */
    public ComponentFixture getInplaceButton(String... xpathVars) {
        String name = xpathVars[0];
        String waitTime = xpathVars[1];
        Locator locator = byXpath("//div[@accessiblename='" + name + "' and @class='EditorTabLabel']//div[@class='InplaceButton']");
        return find(ComponentFixture.class, locator, Duration.ofSeconds(Integer.parseInt(waitTime)));
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
     * Returns the ContainerFixture object associated with the DocumentationHintEditorPane pop-up window.
     *
     * @return The ContainerFixture object associated with the DocumentationHintEditorPane pop-up window.
     */
    public ContainerFixture getDocumentationHintEditorPane() {
        return find(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']//div[@class='DocumentationHintEditorPane']"), Duration.ofSeconds(20));
    }

    /**
     * Returns the ContainerFixture object associated with the DocumentationHintEditorPane pop-up window.
     *
     * @return The ContainerFixture object associated with the DocumentationHintEditorPane pop-up window.
     */
    public ContainerFixture getDiagnosticPane() {
        return find(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']//div[@class='JEditorPane']"), Duration.ofSeconds(5));
    }

    /**
     * Returns the ContainerFixture object associated with the QuickFix "More Actions..." hyperlink for a QuickFix/Code Action
     * section of a diagnostic pop-up window.
     *
     * @return The ContainerFixture object associated with the QuickFix hyperlink of a pop-up window.
     */
    public ContainerFixture getQuickFixMoreActionsLink() {
        return find(ContainerFixture.class, byXpath("//div[@class='JPanel']//div[@class='HyperlinkLabel' and @mytext.key='daemon.tooltip.more.actions.link.label']"), Duration.ofSeconds(20));
    }

    /**
     * Returns the ContainerFixture object associated with the hyperlink for the main action for a QuickFix/Code Action
     * section of a diagnostic pop-up window.
     *
     * @return The ContainerFixture object associated with the QuickFix hyperlink of a pop-up window.
     */
    public ContainerFixture getQuickFixMainActionLink(String visibleText) {
        return find(ContainerFixture.class, byXpath("//div[@class='JPanel']//div[@class='HyperlinkLabel' and contains(@visible_text, '" + visibleText + "')]"), Duration.ofSeconds(20));
    }

    /**
     * Returns the ContainerFixture object associated with the QuickFix/Code Action
     * section of a diagnostic pop-up window.
     *
     * @return The ContainerFixture object associated with the QuickFix portion of a pop-up window.
     */
    public ContainerFixture getQuickFixPane() {
        return find(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']//div[@class='JBViewport'][.//div[@class='MyList']]"), Duration.ofSeconds(5));
    }

    /**
     * Returns the ContainerFixture object associated with the JBTextArea class.
     *
     * @param xpathVars The Locator custom variables: waitTime(seconds)
     * @return The ContainerFixture object associated with the JBTextArea class.
     */
    public ContainerFixture getTextArea(String... xpathVars) {
        String waitTime = xpathVars[0];
        return find(ContainerFixture.class, byXpath("//div[@class='JBTextArea']"), Duration.ofSeconds(Integer.parseInt(waitTime)));
    }

    /**
     * Returns the ContainerFixture object associated with the LookupList class in a HeavyWeightWindow (pop-up window).
     *
     * @return The ContainerFixture object associated with the LookupList class in a HeavyWeightWindow (pop-up window).
     */
    public ContainerFixture getLookupList() {
        return find(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']//div[@class='LookupList']"), Duration.ofSeconds(10));
    }

    /**
     * Returns the ContainerFixture object associated with the MyList class in a HeavyWeightWindow (List window).
     *
     * @return The ContainerFixture object associated with the MyList class in a HeavyWeightWindow (List window).
     */
    public ContainerFixture getMyList() {
        return find(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']//div[@class='MyList']"), Duration.ofSeconds(10));
    }

    /**
     * Returns the ContainerFixture object associated with the RunConfigurationsComboBoxButton class.
     *
     * @return The ContainerFixture object associated with the RunConfigurationsComboBoxButton class.
     */
    public ComponentFixture getRunConfigurationsComboBoxButton() {
        return find(ContainerFixture.class, byXpath("//div[@class='RunConfigurationsComboBoxButton']"), Duration.ofSeconds(5));
    }

    /**
     * Returns true if the associated input component is enabled. False, otherwise.
     *
     * @param component The component fixture to query.
     * @return True if the associated input component is enabled. False, otherwise.
     */
    public boolean isComponentEnabled(ComponentFixture component) {
        return component.callJs("component.isEnabled();", false);
    }
}
