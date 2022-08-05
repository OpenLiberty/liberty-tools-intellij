package io.openliberty.tools.intellij;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.ui.JBUI;
import io.openliberty.tools.intellij.starter.LibertyStarterModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class LibertyModuleWizardStep extends ModuleWizardStep {

    private LibertyStarterModel libertyStarterModel;
    private JPanel rootPanel = new JPanel();
    private JTextField groupIdTextField;
    private JTextField artifactIdTextField;
    private ComboBox<String> mpVersionsComboBox;
    private ComboBox<String> javaVersionsComboBox;

    private ComboBox<String> buildToolComboBox;

    private ComboBox<String> jakartaVersionsComboBox;

    private SpecMatrix specMatrix;

    // initial value stores
    private String initialProjectFieldValue;
    private String initialProjectGroupFieldValue = "com.demo";

    HashMap<String, JSONArray> dependenciesEE2MP;
    HashMap<String, JSONArray> dependenciesMP2EE;

    int ctr = 0;

    String defaultBuild;
    String defaultSE;

    JSONArray optionsBuild;
    JSONArray optionsSE;
    JSONArray optionsEE;
    JSONArray optionsMP;

    public LibertyModuleWizardStep(LibertyStarterModel libertyStarterModel) {
        super();
        this.libertyStarterModel = libertyStarterModel;
    }

    @Override
    public JComponent getComponent() {
        //return new JLabel("Provide some setting here");

        rootPanel = JBUI.Panels.simplePanel(0, 10)
                .addToTop(createTopPanel());

        return rootPanel;
    }

    /**
     * Sets the initial project name that this page will use when created. The name
     * is ignored if the createControl(Composite) method has already been called.
     * Leading and trailing spaces in the name are ignored. Providing the name of an
     * existing project will not necessarily cause the wizard to warn the user.
     * Callers of this method should first check if the project name passed already
     * exists in the workspace.
     *
     * @param name initial project name for this page
     * @see IWorkspace#validateName(String, int)
     */
    /*public void setInitialProjectName(String name) {
        if (name == null) {
            initialProjectFieldValue = null;
        } else {
            initialProjectFieldValue = name.trim();
            if (locationArea != null) {
                locationArea.updateProjectName(name.trim());
            }
        }
    }*/
    private JPanel createTopPanel() {

        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://start.openliberty.io/api/start/info"))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response.body());

            try {
                JSONObject jsonObject = new JSONObject(response.body());
                initialProjectFieldValue = (jsonObject.getJSONObject("a").get("default").toString());
                System.out.println(jsonObject.getJSONObject("a").get("default").toString());
                optionsBuild = jsonObject.getJSONObject("b").getJSONArray("options");
                optionsSE = jsonObject.getJSONObject("j").getJSONArray("options");
                optionsEE = jsonObject.getJSONObject("e").getJSONArray("options");
                optionsMP = jsonObject.getJSONObject("m").getJSONArray("options");
                System.out.println(optionsSE);
                System.out.println(optionsBuild);
                System.out.println(optionsEE);
                System.out.println(optionsMP);

                dependenciesEE2MP = new HashMap<String, JSONArray>();
                dependenciesMP2EE = new HashMap<String, JSONArray>();

                for (int i = 0; i < optionsEE.length(); i++) {
                    JSONArray validMPVersions = jsonObject.getJSONObject("e").getJSONObject("constraints").getJSONObject(optionsEE.getString(i)).getJSONArray("m");
                    System.out.println(optionsEE.getString(i));
                    System.out.println(validMPVersions.toString());
                    dependenciesEE2MP.put(optionsEE.getString(i), validMPVersions);

                    for (int x = 0; x < validMPVersions.length(); x++) {
                        if (!dependenciesMP2EE.containsKey(validMPVersions.getString(x))) {
                            JSONArray arr = new JSONArray();
                            dependenciesMP2EE.put(validMPVersions.getString(x), arr);
                        }
                        dependenciesMP2EE.get(validMPVersions.getString(x)).put(optionsEE.getString(i));
                        System.out.println(validMPVersions.getString(x) + " : " + dependenciesMP2EE.get(validMPVersions.getString(x)));
                    }

                }

                defaultBuild = jsonObject.getJSONObject("b").get("default").toString();
                defaultSE = jsonObject.getJSONObject("j").get("default").toString();

            } catch (JSONException err) {
                System.out.println(err.toString());
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

        JPanel topPanel = new JPanel();
        GridBagLayout topPanelLayout = new GridBagLayout();
        topPanel.setLayout(topPanelLayout);

        GridBagConstraints topPanelLayoutConstraint = new GridBagConstraints();
        topPanelLayoutConstraint.fill = GridBagConstraints.HORIZONTAL;
        topPanelLayoutConstraint.insets = JBUI.insets(2);
        topPanelLayoutConstraint.weightx = 0.25;
        topPanelLayoutConstraint.gridx = 0;

        groupIdTextField = new JTextField("com.example");
        topPanelLayoutConstraint.gridy = 0;
        topPanel.add(new LabeledComponent("GroupId", groupIdTextField), topPanelLayoutConstraint);

        artifactIdTextField = new JTextField("demo");
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("ArtifactId", artifactIdTextField), topPanelLayoutConstraint);

        buildToolComboBox = new ComboBox<String>();
        buildToolComboBox.addItem(defaultBuild);
        try {
            buildToolComboBox.addItem(optionsBuild.getString(1));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("Build Tool", buildToolComboBox), topPanelLayoutConstraint);

        mpVersionsComboBox = new ComboBox<String>();
        try {
            mpVersionsComboBox.addItem(optionsMP.getString(0));
            mpVersionsComboBox.addItem(optionsMP.getString(1));
            mpVersionsComboBox.addItem(optionsMP.getString(2));
            mpVersionsComboBox.addItem(optionsMP.getString(3));
            mpVersionsComboBox.addItem(optionsMP.getString(4));
            mpVersionsComboBox.addItem(optionsMP.getString(5));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("MicroProfile Versions", mpVersionsComboBox), topPanelLayoutConstraint);

        javaVersionsComboBox = new ComboBox<String>();
        javaVersionsComboBox.addItem(defaultSE);
        try {
            javaVersionsComboBox.addItem(optionsSE.getString(0));
            javaVersionsComboBox.addItem(optionsSE.getString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("Java SE Versions", javaVersionsComboBox), topPanelLayoutConstraint);

        jakartaVersionsComboBox = new ComboBox<String>();
        try {
            jakartaVersionsComboBox.addItem(optionsEE.getString(0));
            jakartaVersionsComboBox.addItem(optionsEE.getString(1));
            jakartaVersionsComboBox.addItem(optionsEE.getString(2));
            jakartaVersionsComboBox.addItem(optionsEE.getString(3));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("Java EE/Jakarta EE Versions", jakartaVersionsComboBox), topPanelLayoutConstraint);
        return topPanel;
    }

    /**
     * Populates the data module with the selected values
     */
    @Override
    public void updateDataModel() {
        libertyStarterModel.setGroup(groupIdTextField.getText());
        libertyStarterModel.setArtifact(artifactIdTextField.getText());
        libertyStarterModel.setBuildTool((String) buildToolComboBox.getSelectedItem());
        libertyStarterModel.setJavaVersion((String) javaVersionsComboBox.getSelectedItem());
        libertyStarterModel.setMpVersion((String) mpVersionsComboBox.getSelectedItem());
        libertyStarterModel.setEeVersion((String) jakartaVersionsComboBox.getSelectedItem());
    }

}
