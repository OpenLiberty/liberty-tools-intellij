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
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import com.intellij.remoterobot.utils.RepeatUtilsKt;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.time.Duration;
import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

@DefaultXpath(by = "IdeFrameImpl type", xpath = "//div[@class='IdeFrameImpl']")
@FixtureName(name = "Project Frame")
public class ProjectFrameFixture extends CommonContainerFixture {
    public enum Type {
        ACTIONMENU, SRIPEBUTTON, ACTIONBUTTON, ACTIONMENUITEM, TREE
    }

    public ProjectFrameFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public ComponentFixture getFixtureFromFrame(Type type, String... searchValues) {
        ComponentFixture cf = null;

        switch (type) {
            case ACTIONMENU:
                String amText = searchValues[0];
                cf = find(ComponentFixture.class, byXpath("//div[@class='ActionMenu' and @text='" + amText + "']"), Duration.ofSeconds(10));
                break;
            case ACTIONMENUITEM:
                String amiText = searchValues[0];
                RepeatUtilsKt.waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1), "Wait for Menu items", "No Menu items found", () -> !findAll(ComponentFixture.class, byXpath("//div[@class='ActionMenuItem' and @text='" + amiText + "']")).isEmpty());
                List<ComponentFixture> list = findAll(ComponentFixture.class, byXpath("//div[@class='ActionMenuItem' and @text='" + amiText + "']"));
                cf = list.get(0);
                break;
            case ACTIONBUTTON:
                String abName = searchValues[0];
                String abText = searchValues[1];
                cf = find(ComponentFixture.class, byXpath("//div[@name='" + abName + "']//div[@class='ActionButton' and contains(@myaction.key, '" + abText + "')]"), Duration.ofSeconds(10));
                break;
            case SRIPEBUTTON:
                String sbText = searchValues[0];
                cf = find(ComponentFixture.class, byXpath("//div[@class='StripeButton' and @text='" + sbText + "']"), Duration.ofSeconds(10));
                break;
            case TREE:
                if (searchValues.length == 1) {
                    String tText = searchValues[0];
                    cf = find(ComponentFixture.class, byXpath("//div[@class='Tree' and contains(@visible_text, '" + tText + "')]"), Duration.ofMinutes(2));
                } else if (searchValues.length == 2) {
                    String tName = searchValues[0];
                    String tText = searchValues[1];
                    cf = find(ComponentFixture.class, byXpath("//div[@class='Tree' and @name='" + tName + "' and contains(@visible_text, '" + tText + "')]"), Duration.ofMinutes(6));
                }
                break;
            default:
                Assert.fail("An invalid type of fixture was specified. Project Fixture: " + type);
                break;
        }

        return cf;
    }
}
