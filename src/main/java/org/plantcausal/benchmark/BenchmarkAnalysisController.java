package org.plantcausal.benchmark;

import org.plantcausal.causal.CausalModel;
import org.plantcausal.causal.CausalResult;
import org.plantcausal.data.PlantCausalDataset;

/**
 * Controller connecting benchmark selection and causal execution.
 */
public class BenchmarkAnalysisController {

    private final CausalModel model;

    public BenchmarkAnalysisController(CausalModel model){
        this.model=model;
    }

    public CausalResult run(PlantCausalDataset dataset){
        return model.estimate(dataset);
    }
}
