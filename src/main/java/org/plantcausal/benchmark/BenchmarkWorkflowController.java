package org.plantcausal.benchmark;

import org.plantcausal.data.PlantCausalDataset;
import org.plantcausal.causal.CausalModel;
import org.plantcausal.causal.CausalResult;

/**
 * Connects Benchmark Explorer workflow with causal analysis.
 */
public class BenchmarkWorkflowController {

    private final CausalModel model;

    public BenchmarkWorkflowController(CausalModel model) {
        this.model = model;
    }

    public CausalResult execute(PlantCausalDataset dataset) {
        return model.estimate(dataset);
    }
}
