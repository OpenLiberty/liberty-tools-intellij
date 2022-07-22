package io.openliberty.tools.intellij.;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringUtils;
import org.microshed.intellij.MicroProfileModuleBuilder;
import org.microshed.intellij.model.MicroProfileSpec;
import org.microshed.intellij.model.ModuleInitializationData;
import org.microshed.intellij.model.SpecMatrix;
import org.microshed.intellij.wizard.components.LabeledComponent;
import org.microshed.intellij.wizard.components.MPSpecCheckboxTree;
import org.microshed.intellij.wizard.components.renderers.MPServerComboBoxRenderer;
import org.microshed.intellij.wizard.components.renderers.MPVersionComboBoxRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The first step of the MicroProfile module wizard in which user can choose the JDK, MicroProfile version and the runtime server.
 *
 * @author Ehsan Zaery Moghaddam (zaerymoghaddam@gmail.com)
 */
public class LibertySelectionStep extends ModuleWizardStep {

    private static final Logger LOG = Logger.getInstance("#org.microprofile.starter.intellij.steps.MicroProfileSelectionStep");

    private final ModuleInitializationData moduleCreationData;
    private final WizardContext wizardContext;
    private SpecMatrix specMatrix;

    //  UI Components
    private JPanel rootPanel = new JPanel();
    private ComboBox<String> mpVersionsComboBox;
    private ComboBox<String> mpServersComboBox;
    private MPSpecCheckboxTree mpSpecsCheckboxTree;
    private JLabel specDescriptionLabel;
    private JTextField groupIdTextField;
    private JTextField artifactIdTextField;

    public LibertySelectionStep(ModuleInitializationData moduleCreationData, WizardContext context) {
        this.moduleCreationData = moduleCreationData;
        this.wizardContext = context;
    }

    @Override
    public JComponent getComponent() {
        initializeSpecMatrix();

        //  If we're unable to fetch the spec matrix, just add a label to the form with proper message.
        if (specMatrix == null) {
            rootPanel.add(new JLabel("Fetching MicroProfile specs from " + LibertyModuleBuilder.STARTER_REST_BASE_URL + " failed."));
            return rootPanel;
        }

        rootPanel = JBUI.Panels.simplePanel(0, 10)
                .addToTop(createTopPanel())
                .addToCenter(createCenterPanel())
                .addToBottom(createBottomPanel());

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
        mpVersionsComboBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                String selectedVersion = (String) event.getItem();

                //  Update servers list and specs tree
                List<String> supportedServers = specMatrix.getConfigs().get(selectedVersion).getSupportedServers();
                mpServersComboBox.removeAllItems();
                supportedServers.forEach(mpServersComboBox::addItem);

                List<String> specs = specMatrix.getConfigs().get(selectedVersion).getSpecs();
                mpSpecsCheckboxTree.setModel(specMatrix.getParsedDescription().stream()
                        .filter(spec -> specs.contains(spec.getName()))
                        .collect(Collectors.toList()));
            }
        });
        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("MicroProfile Versions", mpVersionsComboBox), topPanelLayoutConstraint);


        mpServersComboBox = new ComboBox<String>(specMatrix.getConfigs().get(mpVersionsComboBox.getItemAt(mpVersionsComboBox.getSelectedIndex()))
                .getSupportedServers().toArray(new String[0]));
        mpServersComboBox.setRenderer(new MPServerComboBoxRenderer());

        topPanelLayoutConstraint.gridy++;
        topPanel.add(new LabeledComponent("MicroProfile Implementations", mpServersComboBox), topPanelLayoutConstraint);

        return topPanel;
    }


    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(new JLabel("Examples for specifications"), BorderLayout.NORTH);

        JPanel specsListPanel = new JPanel(new BorderLayout(10, 10));
        specsListPanel.setBackground(UIUtil.getListBackground());

        //  CheckboxTree implementation
        List<MicroProfileSpec> specsList = new ArrayList<>();
        if (mpVersionsComboBox.getSelectedItem() != null) {
            List<String> specs = specMatrix.getConfigs().get(mpVersionsComboBox.getItemAt(mpVersionsComboBox.getSelectedIndex())).getSpecs();
            specsList = specMatrix.getParsedDescription().stream()
                    .filter(spec -> specs.contains(spec.getName()))
                    .collect(Collectors.toList());
        }
        mpSpecsCheckboxTree = new MPSpecCheckboxTree(specsList);
        mpSpecsCheckboxTree.addTreeSelectionListener(e -> {
            if((e.getNewLeadSelectionPath() != null) && (e.getNewLeadSelectionPath().getLastPathComponent() != null)) {
                CheckedTreeNode node = (CheckedTreeNode) e.getNewLeadSelectionPath().getLastPathComponent();
                if ((node.getUserObject() != null) && (node.getUserObject() instanceof MicroProfileSpec)) {
                    String description = ((MicroProfileSpec) node.getUserObject()).getDescription();
                    specDescriptionLabel.setText(StringUtils.capitalize(description));
                }
            }
        });
        specsListPanel.add(mpSpecsCheckboxTree, BorderLayout.CENTER);

        centerPanel.add(specsListPanel, BorderLayout.CENTER);

        return centerPanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        specDescriptionLabel = new JLabel();
        specDescriptionLabel.setBorder(JBUI.Borders.empty(5));

        bottomPanel.add(specDescriptionLabel, BorderLayout.NORTH);
        bottomPanel.setBorder(new SideBorder(JBColor.border(), SideBorder.ALL));
        bottomPanel.setPreferredSize(new Dimension(100, 100));

        return bottomPanel;
    }

    /**
     * Fetches the specification matrix and maps them to an instance of {@link SpecMatrix}.
     * <p>
     * TODO: Consider downloading the spec in an asynchronous way and then update the UI. In case of slow internet connection, this slows down the
     * initialization of the new project wizard window.
     */
    private void initializeSpecMatrix() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            specMatrix = mapper.readValue(new URL(MicroProfileModuleBuilder.STARTER_REST_BASE_URL + "/api/2/supportMatrix"), SpecMatrix.class);
        } catch (Exception e) {
            LOG.warn(e);
        }
    }

    @Override
    public boolean validate() throws ConfigurationException {
        if(getSelectedGroupId().trim().isEmpty()) {
            throw new ConfigurationException("GroupId can't be empty");
        }

        if(getSelectedArtifactId().trim().isEmpty()) {
            throw new ConfigurationException("ArtifactId can't be empty");
        }

        return true;
    }

    @Override
    public void updateDataModel() {
        moduleCreationData.setMpVersion(getSelectedMpVersion());
        moduleCreationData.setMpServer(getSelectedMpServer());
        moduleCreationData.setMpSpecs(getSelectedMpSpecs());
        moduleCreationData.setGroupId(getSelectedGroupId());
        moduleCreationData.setArtifactId(getSelectedArtifactId());

        wizardContext.setProjectName(getSelectedArtifactId());
        wizardContext.setDefaultModuleName(getSelectedArtifactId());
    }

    private String getSelectedGroupId() {
        return groupIdTextField.getText().trim();
    }

    private String getSelectedArtifactId() {
        return StringUtil.sanitizeJavaIdentifier(artifactIdTextField.getText().trim());
    }

    private List<String> getSelectedMpSpecs() {
        MicroProfileSpec[] selectedSpecs = mpSpecsCheckboxTree.getCheckedNodes(MicroProfileSpec.class, null);
        return Arrays.stream(selectedSpecs).map(MicroProfileSpec::getName).collect(Collectors.toList());
    }

    private String getSelectedMpServer() {
        return (String) mpServersComboBox.getSelectedItem();
    }

    private String getSelectedMpVersion() {
        return (String) mpVersionsComboBox.getSelectedItem();
    }
}
