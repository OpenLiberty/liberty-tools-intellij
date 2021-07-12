package io.openliberty.tools.intellij.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyPluginIcons;

import java.io.File;
import java.nio.file.Paths;

public class ViewIntegrationTestReport extends LibertyGeneralAction {

    @Override
    protected void executeLibertyAction() {
        setActionCmd("view integration test report");

        // get path to project folder
        final VirtualFile parentFile = buildFile.getParent();
        File failsafeReportFile = Paths.get(parentFile.getCanonicalPath(), "target", "site", "failsafe-report.html").normalize().toAbsolutePath().toFile();
        VirtualFile failsafeReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(failsafeReportFile);


        if (failsafeReportVirtualFile == null || !failsafeReportVirtualFile.exists()) {
            Notification notif = new Notification("Liberty"
                    , LibertyPluginIcons.libertyIcon
                    , "Integration Test Report Does Not Exist"
                    , ""
                    , "Test report (" + failsafeReportFile.getAbsolutePath() + ") does not exist.  " +
                    "Run tests to generate a test report.  Ensure your test report is generating at the correct location."
                    , NotificationType.ERROR
                    , NotificationListener.URL_OPENING_LISTENER);
            Notifications.Bus.notify(notif, project);
            log.debug("Integration test report does not exist at : " + failsafeReportFile.getAbsolutePath());
            return;
        }

        // open test report in browser
        BrowserUtil.browse(failsafeReportVirtualFile.getUrl());
    }

}