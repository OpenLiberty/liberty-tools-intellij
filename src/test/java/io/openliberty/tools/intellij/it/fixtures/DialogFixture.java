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
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.search.locators.Locators;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.time.Duration;
import java.util.Objects;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * Dialog view fixture.
 */
@FixtureName(name = "Dialog")
@DefaultXpath(by = "MyDialog type", xpath = "//div[@class='MyDialog']")
public class DialogFixture extends CommonContainerFixture {

    public enum Type {
        TREE
    }

    public DialogFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    @NotNull
    public static Locator byTitle(@NotNull String title) {
        Intrinsics.checkNotNullParameter(title, "title");
        return Locators.byXpath("title " + title, "//div[@title='" + title + "' and @class='MyDialog']");
    }

    public ComponentFixture getFixtureFromDialog(String text, DialogFixture.Type type) {
        ComponentFixture cf = null;

        if (Objects.requireNonNull(type) == Type.TREE) {
            cf = find(ComponentFixture.class, byXpath("//div[@class='Tree' and contains(@visible_text, '" + text + "')]"), Duration.ofSeconds(10));
        } else {
            Assert.fail("An invalid type of fixture was specified. Dialog Fixture: " + type);
        }

        return cf;
    }

}
