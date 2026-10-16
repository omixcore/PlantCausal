package org.plantcausal.causal;

public class CausalEdge {

    private final CausalNode source;
    private final CausalNode target;

    public CausalEdge(CausalNode source, CausalNode target){
        this.source = source;
        this.target = target;
    }

    public CausalNode getSource(){
        return source;
    }

    public CausalNode getTarget(){
        return target;
    }
}
