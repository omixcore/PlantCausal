package org.plantcausal.gui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Automatic layout model for causal DAG visualization.
 */
public class DAGLayoutEngine {

    public Map<String, double[]> layout(String[] nodes) {
        Map<String, double[]> positions = new LinkedHashMap<>();
        double x = 100;
        double y = 100;
        for (String node : nodes) {
            positions.put(node, new double[]{x, y});
            x += 150;
            y += 80;
        }
        return positions;
    }
}
