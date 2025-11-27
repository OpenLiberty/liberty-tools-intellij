package io.openliberty.tools.intellij.it;

import com.intellij.remoterobot.RemoteRobot;
import com.automation.remarks.junit5.Video;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;


public class MacOSAllowPopupTest {

    /**
     * URL to display the UI Component hierarchy. This is used to obtain xPath related
     * information to find UI components.
     */
    public static final String REMOTE_BOT_URL = "http://localhost:8082";

    /**
     * The remote robot object.
     */
    public static final RemoteRobot remoteRobot = new RemoteRobot(REMOTE_BOT_URL);

    /**
     * Test to handle macOS permission popup if it appears
     */
    @Order(1)
    @Test
    @Video
    @EnabledOnOs({OS.MAC})
    public void AllowPopupTest() {
        UIBotTestUtils.handleMacOSPermissionPopup(remoteRobot);
    }
}
