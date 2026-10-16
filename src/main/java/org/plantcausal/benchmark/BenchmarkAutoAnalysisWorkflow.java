package org.plantcausal.benchmark;

import org.plantcausal.causal.CausalModel;
import org.plantcausal.causal.CausalResult;
import org.plantcausal.data.PlantCausalDataset;

/**
 * End-to-end benchmark execution workflow.
 *
 * Package -> Dataset -> Causal Model -> Report
 */
public class BenchmarkAutoAnalysisWorkflow {

    private final CausalModel model;

    public BenchmarkAutoAnalysisWorkflow(CausalModel model) {
        this.model = model;
    }

    public CausalResult run(PlantCausalDataset dataset) {
        if (model == null) {
            throw new IllegalStateException("No causal model configured");
        }

        return model.estimate(dataset);
    }
}
