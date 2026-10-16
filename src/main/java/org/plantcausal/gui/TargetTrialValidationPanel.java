package org.plantcausal.gui;

import org.plantcausal.schema.TargetTrial;
import org.plantcausal.schema.ValidationResult;
import org.plantcausal.service.ValidationService;

import javax.swing.*;
import java.awt.*;

public class TargetTrialValidationPanel extends JPanel {

    private final JTextArea reportArea = new JTextArea();
    private final ValidationService validator = new ValidationService();

    public TargetTrialValidationPanel(){
        setLayout(new BorderLayout());

        JButton validate = new JButton("RUN VALIDATION");

        validate.addActionListener(e -> {
            ValidationResult result = validator.validate(createDemoTrial());
            reportArea.setText(result.toString());
        });

        add(validate, BorderLayout.NORTH);
        add(new JScrollPane(reportArea), BorderLayout.CENTER);
    }

    private TargetTrial createDemoTrial(){
        return new TargetTrial();
    }
}
