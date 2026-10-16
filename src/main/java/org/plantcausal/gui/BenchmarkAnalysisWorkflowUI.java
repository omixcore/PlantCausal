package org.plantcausal.gui;

/**
 * High-level workflow controller for benchmark analysis.
 */
public class BenchmarkAnalysisWorkflowUI {

    public String[] workflowSteps() {
        return new String[]{
            "Load Benchmark Package",
            "Validate Dataset",
            "Generate DAG",
            "Select Causal Model",
            "Run Analysis",
            "Display Report"
        };
    }
}
