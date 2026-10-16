package org.plantcausal.causal;

import java.util.ArrayList;
import java.util.List;

public class CausalGraph {

    private final List<CausalNode> nodes = new ArrayList<>();
    private final List<CausalEdge> edges = new ArrayList<>();

    public void addNode(CausalNode node){
        nodes.add(node);
    }

    public void addEdge(CausalEdge edge){
        edges.add(edge);
    }

    public void addEdge(String source, String target) {
        edges.add(new CausalEdge(node(source), node(target)));
    }
    private CausalNode node(String name) {
        for (CausalNode node : nodes) if (node.getName().equals(name)) return node;
        CausalNode node = new CausalNode(name, "variable"); nodes.add(node); return node;
    }
    public List<CausalNode> getNodes(){
        return nodes;
    }

    public List<CausalEdge> getEdges(){
        return edges;
    }
}
