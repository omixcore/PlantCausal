package org.plantcausal.benchmark;

import org.plantcausal.data.PlantCausalDataset;
import org.plantcausal.causal.CausalModel;
import org.plantcausal.causal.CausalResult;

public class BenchmarkAnalysisPipeline {

    private final CausalModel model;

    public BenchmarkAnalysisPipeline(CausalModel model) {
        this.model = model;
    }

    public CausalResult run(PlantCausalDataset dataset) {
        return model.estimate(dataset);
    }
}
