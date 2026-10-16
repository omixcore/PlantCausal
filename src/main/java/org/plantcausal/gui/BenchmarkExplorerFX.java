package org.plantcausal.gui;

/**
 * JavaFX workflow entry point for Benchmark Explorer.
 *
 * Workflow:
 * Benchmark -> Validation -> DAG -> Model -> Report
 */
public class BenchmarkExplorerFX {

    public String[] workflowSteps() {
        return new String[]{
                "Select Benchmark",
                "Validate Package",
                "Generate DAG",
                "Select Model",
                "Run Analysis",
                "View Causal Report"
        };
    }
}
