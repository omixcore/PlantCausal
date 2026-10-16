package org.plantcausal.gui;

import javax.swing.*;
import java.awt.*;

public class BenchmarkExplorerPanel extends JPanel {

    private final DefaultListModel<String> model = new DefaultListModel<>();

    public BenchmarkExplorerPanel(){
        setLayout(new BorderLayout());

        model.addElement("maize_salt_stress_demo");
        model.addElement("drought_stress_demo");

        add(new JLabel("Benchmark Explorer"), BorderLayout.NORTH);
        add(new JScrollPane(new JList<>(model)), BorderLayout.CENTER);
    }
}
