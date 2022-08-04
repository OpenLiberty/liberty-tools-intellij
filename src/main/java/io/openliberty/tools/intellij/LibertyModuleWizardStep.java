package io.openliberty.tools.intellij;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.ui.JBUI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class LibertyModuleWizardStep extends ModuleWizardStep {

    private JPanel rootPanel = new JPanel();
    private JTextField groupIdTextField;
    private JTextField artifactIdTextField;
    private ComboBox<String> mpVersionsComboBox;
    private ComboBox<String> javaVersionsComboBox;

    private ComboBox<String> jakartaVersionsComboBox;

    private SpecMatrix specMatrix;

    // initial value stores
    private String initialProjectFieldValue;
    private String initialProjectGroupFieldValue = "com.demo";

    JSONArray optionsBuild;
    JSONArray optionsSE;
    JSONArray optionsEE;
    JSONArray optionsMP;

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
     *
     * @see IWorkspace#validateName(String, int)
     *
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

        /*HttpClient client = HttpClient.newHttpClient();
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
                setInitialProjectName(jsonObject.getJSONObject("a").get("default").toString());
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

                for(int i = 0; i < optionsEE.length(); i++) {
                    JSONArray validMPVersions = jsonObject.getJSONObject("e").getJSONObject("constraints").getJSONObject(optionsEE.getString(i)).getJSONArray("m");
                    System.out.println(optionsEE.getString(i));
                    System.out.println(validMPVersions.toString());
                    dependenciesEE2MP.put(optionsEE.getString(i), validMPVersions);

                    for(int x = 0; x < validMPVersions.length(); x++) {
                        if(!dependenciesMP2EE.containsKey(validMPVersions.getString(x))) {
                            JSONArray arr = new JSONArray();
                            dependenciesMP2EE.put(validMPVersions.getString(x), arr);
                        }
                        dependenciesMP2EE.get(validMPVersions.getString(x)).put(optionsEE.getString(i));
                        System.out.println(validMPVersions.getString(x) + " : " + dependenciesMP2EE.get(validMPVersions.getString(x)));
                    }

                }

                defaultBuild = jsonObject.getJSONObject("b").get("default").toString();
                defaultSE = jsonObject.getJSONObject("j").get("default").toString();

            }catch (JSONException err){
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
        }*/

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

        mpVersionsComboBox = new ComboBox<String>();
        mpVersionsComboBox.addItem("Test1");
        mpVersionsComboBox.addItem("Test2");
        mpVersionsComboBox.addItem("Test3");
        mpVersionsComboBox.setRenderer(new MPVersionComboBoxRenderer());
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("MicroProfile Versions", mpVersionsComboBox), topPanelLayoutConstraint);

        javaVersionsComboBox = new ComboBox<String>();
        javaVersionsComboBox.addItem("Test1");
        javaVersionsComboBox.addItem("Test2");
        javaVersionsComboBox.addItem("Test3");
        javaVersionsComboBox.setRenderer(new MPVersionComboBoxRenderer());
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("Java Versions", javaVersionsComboBox), topPanelLayoutConstraint);

        jakartaVersionsComboBox = new ComboBox<String>();
        jakartaVersionsComboBox.addItem("Test1");
        jakartaVersionsComboBox.addItem("Test2");
        jakartaVersionsComboBox.addItem("Test3");
        jakartaVersionsComboBox.setRenderer(new MPVersionComboBoxRenderer());
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("Jakarta Versions", jakartaVersionsComboBox), topPanelLayoutConstraint);


        return topPanel;
    }

        @Override
        public void updateDataModel () {
            //todo update model according to UI
        }

}
