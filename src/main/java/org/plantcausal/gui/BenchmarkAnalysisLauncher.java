package org.plantcausal.gui;

import org.plantcausal.data.PlantCausalDataset;
import org.plantcausal.causal.CausalModel;
import org.plantcausal.causal.CausalResult;

/**
 * Controller entry point for benchmark one-click analysis workflow.
 */
public class BenchmarkAnalysisLauncher {

    private final CausalModel model;

    public BenchmarkAnalysisLauncher(CausalModel model) {
        this.model = model;
    }

    public CausalResult run(PlantCausalDataset dataset) {
        return model.estimate(dataset);
    }
}
