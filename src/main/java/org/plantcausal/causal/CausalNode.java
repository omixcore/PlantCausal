package org.plantcausal.causal;

public class CausalNode {

    private String name;
    private String type;

    public CausalNode(String name, String type){
        this.name = name;
        this.type = type;
    }

    public String getName(){
        return name;
    }

    public String getType(){
        return type;
    }
}
