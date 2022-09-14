package io.openliberty.tools.intellij.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;

import java.io.File;
import java.nio.file.Paths;
import java.text.MessageFormat;

public class ViewIntegrationTestReport extends LibertyGeneralAction {

    public ViewIntegrationTestReport() {
        setActionCmd(LocalizedResourceUtil.getMessage("view.integration.test.report"));
    }

    @Override
    protected void executeLibertyAction() {
        // get path to project folder
        final VirtualFile parentFile = buildFile.getParent();
        File failsafeReportFile = Paths.get(parentFile.getCanonicalPath(), "target", "site", "failsafe-report.html").normalize().toAbsolutePath().toFile();
        VirtualFile failsafeReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(failsafeReportFile);


        if (failsafeReportVirtualFile == null || !failsafeReportVirtualFile.exists()) {
            Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID
                    , LibertyPluginIcons.libertyIcon
                    , LocalizedResourceUtil.getMessage("integration.test.report.does.not.exist.notification.title")
                    , ""
                    , LocalizedResourceUtil.getMessage("test.report.does.not.exist", failsafeReportFile.getAbsolutePath())
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