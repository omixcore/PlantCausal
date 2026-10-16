package org.plantcausal.gui;

import javax.swing.*;

public class ProjectWorkflowPanel extends JPanel {

    public ProjectWorkflowPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new JLabel("PlantCausal Project Workflow"));
        add(new JButton("Load Target Trial Package"));
        add(new JButton("Validate Data Package"));
        add(new JButton("Export Validation Report"));
    }
}
