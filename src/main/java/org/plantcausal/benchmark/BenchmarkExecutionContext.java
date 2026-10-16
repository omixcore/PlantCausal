package org.plantcausal.benchmark;

import org.plantcausal.causal.CausalResult;
import org.plantcausal.data.PlantCausalDataset;

/**
 * Context object connecting benchmark loading and causal analysis.
 */
public class BenchmarkExecutionContext {

    private PlantCausalDataset dataset;
    private CausalResult result;

    public PlantCausalDataset getDataset() {
        return dataset;
    }

    public void setDataset(PlantCausalDataset dataset) {
        this.dataset = dataset;
    }

    public CausalResult getResult() {
        return result;
    }

    public void setResult(CausalResult result) {
        this.result = result;
    }
}
