package io.openliberty.tools.intellij;
/*package io.openliberty.tools.intellij;


import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.actions.MavenActionUtil;
import org.microshed.intellij.model.ModuleInitializationData;
import org.microshed.intellij.wizard.MicroProfileSelectionStep;

import javax.swing.*;

/**
 * The build is extending from Java to ensure it will be like any normal Java module. However it uses the {@link MicroProfileCompatibleJavaModuleType}
 * to ensure it can limit the possible JDKs to JDK 8. This can be removed if later we add support for JDK 11 to MicroProfile Starter.
 *
 * @author Ehsan Zaery Moghaddam (zaerymoghaddam@gmail.com)
 */
/*public class LibertyModuleBuilder extends JavaModuleBuilder {

    public static final String STARTER_REST_BASE_URL = "https://start.microprofile.io";
    private static final Logger LOG = Logger.getInstance("#org.microprofile.starter.intellij.MicroProfileModuleBuilder");
    private final ModuleInitializationData moduleCreationData = new ModuleInitializationData();

    @Override
    public ModuleType getModuleType() {
        return LibertyCompatibleJavaModuleType.getModuleType();
    }

    @Override
    public String getParentGroup() {
        return JavaModuleType.JAVA_GROUP;
    }

    @Override
    public Icon getNodeIcon() {
        return LibertyPluginIcons.libertyIcon;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @Override
    public String getDescription() {
        return "A MicroProfile module is used to start developing microservices using MicroProfile. It uses MicroProfile Starter to setup proper " +
                "dependencies and configurations based on the MicroProfile runtime that you choose.";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getPresentableName() {
        return "MicroProfile Starter";
    }

    @Override
    public int getWeight() {
        //  This ensures the "MicroProfile Starter" appears right after Java/Java EE in Ultimate version
        return 95;
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new LibertySelectionStep(moduleCreationData, context);
    }

    /**
     * By overwriting this method, we ensure that the artifactId chosen by the user would be used as the default value for the folder name.
     *
     * @param settingsStep step to be modified
     * @return callback ({@link ModuleWizardStep#validate()} and {@link ModuleWizardStep#updateDataModel()} will be invoked)
     */
   /* @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        ModuleNameLocationSettings moduleNameLocationSettings = settingsStep.getModuleNameLocationSettings();
        if (moduleNameLocationSettings != null && moduleCreationData.getArtifactId() != null) {
            moduleNameLocationSettings.setModuleName(StringUtil.sanitizeJavaIdentifier(moduleCreationData.getArtifactId()));
        }

        return super.modifySettingsStep(settingsStep);
    }
}*/

import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import io.openliberty.tools.intellij.starter.LibertyStarterModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.swing.*;

public class LibertyModuleBuilder extends ModuleBuilder {

    private LibertyStarterModel libertyStarterModel = new LibertyStarterModel();

    /**
     * Call the Open Liberty Starter API to generate a project with the selected values
     *
     * @param modifiableRootModel
     * @throws ConfigurationException
     */
    @Override
    public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) throws ConfigurationException {
        super.setupRootModel(modifiableRootModel);

        HttpClient Client = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://start.openliberty.io/api/start?a=" + libertyStarterModel.getArtifact()
                            + "&b=" + libertyStarterModel.getBuildTool()
                            + "&e=" + libertyStarterModel.getEeVersion()
                            + "&g=" + libertyStarterModel.getBuildTool()
                            + "&j=" + libertyStarterModel.getJavaVersion()
                            + "&m=" + libertyStarterModel.getMpVersion()))
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    Client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            InputStream in = new BufferedInputStream(response.body());
            String home = System.getProperty("user.home");
            String contentPath = getContentEntryPath(); // file directory of the module created
            // TODO update to use project name
            File file = new File(contentPath, "demo.zip");

            try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
                int read;
                byte[] bytes = new byte[8192];
                while ((read = in.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            }
        } catch (URISyntaxException e) {
            System.out.println("uhoh");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("uhoh2");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("uhoh3");
            e.printStackTrace();
        }

    }

    /**
     * Use artifact id chosen by user as default value for project name
     *
     * @param settingsStep
     * @return
     */
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        ModuleNameLocationSettings moduleNameLocationSettings = settingsStep.getModuleNameLocationSettings();
        if (moduleNameLocationSettings != null && libertyStarterModel.getArtifact() != null) {
            moduleNameLocationSettings.setModuleName(libertyStarterModel.getArtifact());
        }
        return super.modifySettingsStep(settingsStep);
    }

    @Override
    public LibertyModuleType getModuleType() {
        return LibertyModuleType.getInstance();
        //TODO: change module type
    }

    /*@Override
    public ModuleWizardStep[] createWizardSteps(
            @NotNull WizardContext wizardContext,
            @NotNull ModulesProvider modulesProvider) {
        return new ModuleWizardStep[]{new ModuleWizardStep() {
            @Override
            public JComponent getComponent() {
                return new JLabel("Put your content here");
            }

            @Override
            public void updateDataModel() {

            }
        }};
    }*/

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new LibertyModuleWizardStep(libertyStarterModel);
    }
}