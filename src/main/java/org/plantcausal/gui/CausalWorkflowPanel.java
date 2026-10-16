package org.plantcausal.gui;

import javax.swing.*;
import java.awt.*;

public class CausalWorkflowPanel extends JPanel {

    public CausalWorkflowPanel(){
        setLayout(new BorderLayout());

        JPanel controls=new JPanel();
        controls.add(new JLabel("Treatment:"));
        controls.add(new JTextField("treatment",12));
        controls.add(new JLabel("Outcome:"));
        controls.add(new JTextField("outcome",12));
        controls.add(new JButton("Run Causal Analysis"));

        add(controls,BorderLayout.NORTH);
        add(new JTextArea("CausalResult report will appear here."),BorderLayout.CENTER);
    }
}
