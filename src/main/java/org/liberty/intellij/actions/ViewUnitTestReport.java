package org.liberty.intellij.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.liberty.intellij.util.Constants;
import org.liberty.intellij.util.LibertyProjectUtil;

import java.io.File;
import java.nio.file.Paths;

public class ViewUnitTestReport extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) return;

        final VirtualFile file = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (file == null) return;

        // get path to project folder
        final VirtualFile parentFile = file.getParent();
        File surefireReportFile = Paths.get(parentFile.getCanonicalPath(), "target", "site", "surefire-report.html").normalize().toAbsolutePath().toFile();
        VirtualFile surefireReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(surefireReportFile);

        if (surefireReportVirtualFile == null || !surefireReportVirtualFile.exists()) {
            Messages.showErrorDialog(project, "Test report (" + surefireReportFile.getAbsolutePath() + ") does not exist.  " +
                            "Run tests to generate a test report.  Ensure your test report is generating at the correct location.",
                    "Unit Test Report Does Not Exist");
            return;
        }

        // open test report in browser
        BrowserUtil.browse(surefireReportVirtualFile.getUrl());
    }
}