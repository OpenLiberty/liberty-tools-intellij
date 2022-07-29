package io.openliberty.tools.intellij;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class LibertyModuleWizardStep extends ModuleWizardStep {

    private JPanel rootPanel = new JPanel();
    private JTextField groupIdTextField;
    private JTextField artifactIdTextField;
    private ComboBox<String> mpVersionsComboBox;

    private SpecMatrix specMatrix;

    @Override
    public JComponent getComponent() {
        //return new JLabel("Provide some setting here");

        rootPanel = JBUI.Panels.simplePanel(0, 10)
                .addToTop(createTopPanel());

        return rootPanel;
    }

    private JPanel createTopPanel() {
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

        mpVersionsComboBox = new ComboBox<String>(specMatrix.getConfigs().keySet().toArray(new String[0]));
        mpVersionsComboBox.setRenderer(new MPVersionComboBoxRenderer());
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("MicroProfile Versions", mpVersionsComboBox), topPanelLayoutConstraint);
        return topPanel;
    }

        @Override
        public void updateDataModel () {
            //todo update model according to UI
        }

}
