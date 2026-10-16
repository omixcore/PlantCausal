package org.plantcausal.causal;

import org.plantcausal.schema.TargetTrial;

public class TargetTrialDAGGenerator {

    public CausalGraph generate(TargetTrial trial){
        CausalGraph graph = new CausalGraph();

        if(trial == null){
            return graph;
        }

        graph.addNode(new CausalNode("Treatment", "intervention"));
        graph.addNode(new CausalNode("Outcome", "phenotype"));

        graph.addEdge("Treatment", "Outcome");

        if(trial.getPopulation()!=null){
            graph.addNode(new CausalNode("Genotype", "covariate"));
            graph.addEdge("Genotype", "Outcome");
        }

        return graph;
    }
}
