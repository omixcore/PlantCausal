package org.plantcausal.causal;

public class DAGBuilder {

    private final CausalGraph graph;

    public DAGBuilder() {
        this.graph = new CausalGraph();
    }

    public DAGBuilder addRelation(String source, String target) {
        graph.addEdge(source, target);
        return this;
    }

    public CausalGraph build() {
        return graph;
    }
}
