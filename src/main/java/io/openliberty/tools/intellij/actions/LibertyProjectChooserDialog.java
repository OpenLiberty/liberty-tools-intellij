package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.ui.ComboBox;

import java.util.List;
import javax.swing.*;
import java.awt.*;

import com.intellij.openapi.ui.messages.MessageDialog;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class LibertyProjectChooserDialog extends MessageDialog {
    //private JPanel contentPane;
    //private JButton buttonOK;
    //private JButton buttonCancel;
    private ComboBox<String> myComboBox;

    public LibertyProjectChooserDialog(@Nullable Project project,
                                       @NlsContexts.DialogMessage String message,
                                       @NlsContexts.DialogTitle String title,
                                       @Nullable Icon icon,
                                       String[] values,
                                       String[] tooltips,
                                       @NlsSafe String initialValue) {
        super(message, title, new String[]{Messages.getOkButton(), Messages.getCancelButton()}, 0, icon);
        myComboBox.setModel(new DefaultComboBoxModel<>(values));
        myComboBox.setSelectedItem(initialValue);
        ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
        renderer.setTooltips(List.of(tooltips));
        myComboBox.setRenderer(renderer);
    }

    public int getSelectedIndex() {
        if (getExitCode() == 0) {
            return myComboBox.getSelectedIndex();
        }
        return -1;
    }

    @Override
    protected JComponent createCenterPanel() {
        return null;
    }

    @Override
    protected JComponent createNorthPanel() {
        JPanel panel = createIconPanel();
        JPanel messagePanel = createMessagePanel();

        myComboBox = new ComboBox<>(220);
        messagePanel.add(myComboBox, BorderLayout.SOUTH);
        panel.add(messagePanel, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected void doOKAction() {
        String inputString = myComboBox.getSelectedItem().toString().trim();
        //if (myValidator == null || myValidator.checkInput(inputString) && myValidator.canClose(inputString)) {
            super.doOKAction();
        //}
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myComboBox;
    }
}

class ComboboxToolTipRenderer extends DefaultListCellRenderer {
    List<String> tooltips;

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {

        JComponent comp = (JComponent) super.getListCellRendererComponent(list,
                value, index, isSelected, cellHasFocus);

        if (-1 < index && null != value && null != tooltips) {
            list.setToolTipText(tooltips.get(index));
        }
        return comp;
    }

    public void setTooltips(List<String> tooltips) {
        this.tooltips = tooltips;
    }
}
