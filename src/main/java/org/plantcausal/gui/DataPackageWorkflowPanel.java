package org.plantcausal.gui;

import org.plantcausal.validator.DataPackageValidator;
import org.plantcausal.schema.ValidationResult;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class DataPackageWorkflowPanel extends JPanel {

    private final JTextArea reportArea = new JTextArea();

    public DataPackageWorkflowPanel() {
        setLayout(new BorderLayout());

        JButton validate = new JButton("Validate Data Package");
        validate.addActionListener(e -> runValidation());

        add(validate, BorderLayout.NORTH);
        add(new JScrollPane(reportArea), BorderLayout.CENTER);
    }

    private void runValidation() {
        ValidationResult result = new DataPackageValidator()
                .validate(Path.of("."));

        reportArea.setText(result.toString());
    }
}
