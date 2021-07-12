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

public class ViewUnitTestReport extends LibertyGeneralAction {

    @Override
    protected void executeLibertyAction() {
        setActionCmd("view unit test report");
        // get path to project folder
        final VirtualFile parentFile = buildFile.getParent();
        File surefireReportFile = Paths.get(parentFile.getCanonicalPath(), "target", "site", "surefire-report.html").normalize().toAbsolutePath().toFile();
        VirtualFile surefireReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(surefireReportFile);

        if (surefireReportVirtualFile == null || !surefireReportVirtualFile.exists()) {
            Notification notif = new Notification("Liberty"
                    , LibertyPluginIcons.libertyIcon
                    , "Unit Test Report Does Not Exist"
                    , ""
                    , "Test report (" + surefireReportFile.getAbsolutePath() + ") does not exist.  " +
                    "Run tests to generate a test report.  Ensure your test report is generating at the correct location."
                    , NotificationType.ERROR
                    , NotificationListener.URL_OPENING_LISTENER);
            Notifications.Bus.notify(notif, project);
            log.debug("Unit test report does not exist at : " + surefireReportFile.getAbsolutePath());
            return;
        }

        // open test report in browser
        BrowserUtil.browse(surefireReportVirtualFile.getUrl());
    }

}