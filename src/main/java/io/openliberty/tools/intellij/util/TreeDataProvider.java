package io.openliberty.tools.intellij.util;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TreeDataProvider implements DataProvider {

    public VirtualFile currentFile;
    public String projectName;
    public String projectType;

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (dataId.equals(Constants.LIBERTY_BUILD_FILE)) {
            if (this.currentFile != null) {
                return this.currentFile;
            }
        } else if (dataId.equals(Constants.LIBERTY_PROJECT_NAME)) {
            return this.projectName;
        } else if (dataId.equals(Constants.LIBERTY_PROJECT_TYPE)) {
            return this.projectType;
        }
        return null;
    }

    public void saveData(@NotNull VirtualFile file, @NotNull String projectName, @NotNull String projectType) {
        this.currentFile = file;
        this.projectName = projectName;
        this.projectType = projectType;
    }

}
