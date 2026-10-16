package org.plantcausal.gui;

import javax.swing.*;
import java.awt.*;

public class CausalAnalysisPanel extends JPanel {

    public CausalAnalysisPanel(){
        setLayout(new GridLayout(3,2));

        add(new JLabel("Treatment"));
        add(new JTextField("treatment"));

        add(new JLabel("Outcome"));
        add(new JTextField("outcome"));

        add(new JButton("Run Causal Analysis"));
    }
}
