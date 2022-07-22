package io.openliberty.tools.intellij;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.lang.JavaVersion;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LibertyCompatibleJavaModuleType extends ModuleType<LibertyProfileModuleBuilder>{
    private static final String LIBERTY_MODULE_ID = "LIBERTY_MODULE";

    public LibertyCompatibleJavaModuleType() {
        super(LIBERTY_MODULE_ID);
    }

    protected LibertyCompatibleJavaModuleType(@NotNull String id) {
        super(id);
    }

    public static ModuleType getModuleType() {
        return ModuleTypeManager.getInstance().findByID(LIBERTY_MODULE_ID);
    }

    @NotNull
    @Override
    public LibertyCompatibleJavaModuleType createModuleBuilder() {
        return new LibertyCompatibleJavaModuleType();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @NotNull
    @Override
    public String getName() {
        return "Liberty Starter";
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getDescription() {
        return "A MicroProfile module is used to start developing microservices using MicroProfile. It uses MicroProfile Starter to setup proper " +
                "dependencies and configurations based on the MicroProfile runtime that you choose.";
    }

    @NotNull
    @Override
    public Icon getNodeIcon(boolean isOpened) {
        return LibertyPluginIcons.libertyIcon;
    }

    @Nullable
    @Override
    public ModuleWizardStep modifyProjectTypeStep(
            @NotNull SettingsStep settingsStep,
            @NotNull ModuleBuilder moduleBuilder) {
        return new JavaSettingsStep(settingsStep, moduleBuilder, moduleBuilder::isSuitableSdkType) {
            @Override
            public boolean validate() throws ConfigurationException {
                boolean result = super.validate();

                if (result) {
                    Sdk jdk = myJdkComboBox.getSelectedJdk();
                    if (jdk != null && jdk.getVersionString() != null) {
                        JavaVersion javaVersion = JavaVersion.parse(jdk.getVersionString());
                        if (javaVersion.feature != 8) {
                            Messages.showErrorDialog("MicroProfile Starter requires JDK 8", "");
                            result = false;
                        }
                    } else {
                        Messages.showErrorDialog("MicroProfile Starter requires JDK 8", "");
                        result = false;
                    }
                }

                return result;
            }
        };
    }
}

}