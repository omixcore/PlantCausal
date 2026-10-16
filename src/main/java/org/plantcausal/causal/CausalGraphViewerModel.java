package org.plantcausal.causal;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model used by GUI layers to render causal DAG structures.
 */
public class CausalGraphViewerModel {

    private final List<CausalNode> nodes = new ArrayList<>();
    private final List<CausalEdge> edges = new ArrayList<>();

    public void addNode(CausalNode node) {
        nodes.add(node);
    }

    public void addEdge(CausalEdge edge) {
        edges.add(edge);
    }

    public List<CausalNode> getNodes() {
        return nodes;
    }

    public List<CausalEdge> getEdges() {
        return edges;
    }
}
