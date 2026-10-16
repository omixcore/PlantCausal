package org.plantcausal.gui;

import java.util.HashMap;
import java.util.Map;

/**
 * Interactive DAG viewer model for JavaFX integration.
 */
public class InteractiveDAGViewer {

    private final Map<String, double[]> positions = new HashMap<>();

    public void addNode(String id, double x, double y) {
        positions.put(id, new double[]{x, y});
    }

    public void moveNode(String id, double x, double y) {
        if (positions.containsKey(id)) {
            positions.put(id, new double[]{x, y});
        }
    }

    public Map<String, double[]> getPositions() {
        return positions;
    }
}
