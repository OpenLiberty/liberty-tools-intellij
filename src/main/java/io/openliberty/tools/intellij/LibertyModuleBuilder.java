package io.openliberty.tools.intellij;


import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;
import io.openliberty.tools.intellij.starter.LibertyStarterModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;



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

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new LibertyModuleWizardStep(libertyStarterModel);
    }
}