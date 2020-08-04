package io.openliberty.tools.intellij.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;

import java.io.File;
import java.nio.file.Paths;

public class ViewIntegrationTestReport extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Logger log = Logger.getInstance(ViewIntegrationTestReport.class);;

        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) {
            log.debug("Unable to view integration test report, could not resolve project");
            return;
        }

        final VirtualFile file = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (file == null) {
            log.debug("Unable to view integration test report, could not resolve configuration file for  " + project.getName());
            return;
        }

        // get path to project folder
        final VirtualFile parentFile = file.getParent();
        File failsafeReportFile = Paths.get(parentFile.getCanonicalPath(), "target", "site", "failsafe-report.html").normalize().toAbsolutePath().toFile();
        VirtualFile failsafeReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(failsafeReportFile);


        if (failsafeReportVirtualFile == null || !failsafeReportVirtualFile.exists()) {
            Notification notif = new Notification("Liberty Dev Dashboard"
                    , Constants.libertyIcon
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