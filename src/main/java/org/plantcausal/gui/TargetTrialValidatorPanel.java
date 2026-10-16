package org.plantcausal.gui;

import javax.swing.*;
import java.awt.*;

public class TargetTrialValidatorPanel extends JPanel {

    private final JTextArea reportArea = new JTextArea();

    public TargetTrialValidatorPanel() {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Target Trial Validation");
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        reportArea.setEditable(false);
        reportArea.setText("Load a Target Trial package and run validation.");

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(reportArea), BorderLayout.CENTER);
    }

    public void showReport(String report) {
        reportArea.setText(report);
    }
}
