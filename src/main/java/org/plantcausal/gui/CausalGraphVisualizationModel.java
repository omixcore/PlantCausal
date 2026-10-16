package org.plantcausal.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model for JavaFX causal graph visualization.
 */
public class CausalGraphVisualizationModel {

    private final List<String> nodes = new ArrayList<>();
    private final List<String[]> edges = new ArrayList<>();

    public void addNode(String node) {
        nodes.add(node);
    }

    public void addEdge(String source, String target) {
        edges.add(new String[]{source, target});
    }

    public List<String> getNodes() {
        return nodes;
    }

    public List<String[]> getEdges() {
        return edges;
    }
}
