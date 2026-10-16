package org.plantcausal.gui;

import javax.swing.*;

/**
 * Main PlantCausal workflow container.
 */
public class MainWorkflowWindow extends JFrame {

    public MainWorkflowWindow() {
        setTitle("PlantCausal");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Project", new JPanel());
        tabs.addTab("Design", new JPanel());
        tabs.addTab("Data", new JPanel());
        tabs.addTab("Validation", new JPanel());
        tabs.addTab("Analysis", new JPanel());

        add(tabs);
    }
}
