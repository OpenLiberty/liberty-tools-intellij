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
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.annotations.NotNull;
import io.openliberty.tools.intellij.util.Constants;

import java.io.File;
import java.nio.file.Paths;

public class ViewUnitTestReport extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Logger log = Logger.getInstance(ViewUnitTestReport.class);;
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) return;

        final VirtualFile file = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (file == null) return;

        // get path to project folder
        final VirtualFile parentFile = file.getParent();
        File surefireReportFile = Paths.get(parentFile.getCanonicalPath(), "target", "site", "surefire-report.html").normalize().toAbsolutePath().toFile();
        VirtualFile surefireReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(surefireReportFile);

        if (surefireReportVirtualFile == null || !surefireReportVirtualFile.exists()) {
            Notification notif = new Notification("Liberty Dev Dashboard"
                    , Constants.libertyIcon
                    , "Unit Test Report Does Not Exist"
                    , ""
                    , "Test report (" + surefireReportFile.getAbsolutePath() + ") does not exist.  " +
                    "Run tests to generate a test report.  Ensure your test report is generating at the correct location."
                    , NotificationType.ERROR
                    , NotificationListener.URL_OPENING_LISTENER);
            Notifications.Bus.notify(notif, project);
            log.debug("Gradle test report does not exist at : " + surefireReportFile.getAbsolutePath());
            return;
        }

        // open test report in browser
        BrowserUtil.browse(surefireReportVirtualFile.getUrl());
    }
}