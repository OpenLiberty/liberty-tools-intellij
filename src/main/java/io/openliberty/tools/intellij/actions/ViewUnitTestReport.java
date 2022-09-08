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

import java.io.File;
import java.nio.file.Paths;
import java.text.MessageFormat;

import static io.openliberty.tools.intellij.util.Constants.LibertyRB;

public class ViewUnitTestReport extends LibertyGeneralAction {

    public ViewUnitTestReport() {
        setActionCmd(LibertyRB.getString("view.unit.test.report"));
    }

    @Override
    protected void executeLibertyAction() {
        // get path to project folder
        final VirtualFile parentFile = buildFile.getParent();
        File surefireReportFile = Paths.get(parentFile.getCanonicalPath(), "target", "site", "surefire-report.html").normalize().toAbsolutePath().toFile();
        VirtualFile surefireReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(surefireReportFile);

        if (surefireReportVirtualFile == null || !surefireReportVirtualFile.exists()) {
            Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID
                    , LibertyPluginIcons.libertyIcon
                    , LibertyRB.getString("unit.test.report.does.not.exist")
                    , ""
                    , MessageFormat.format(LibertyRB.getString("test.report.does.not.exist"), surefireReportFile.getAbsolutePath())
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