package org.plantcausal.gui;

public class BenchmarkAnalysisDashboard {

    private String selectedBenchmark;
    private String treatment;
    private String outcome;
    private String model;

    public void selectBenchmark(String benchmark) {
        this.selectedBenchmark = benchmark;
    }

    public void configure(String treatment, String outcome, String model) {
        this.treatment = treatment;
        this.outcome = outcome;
        this.model = model;
    }

    public String getWorkflowStatus() {
        return "Benchmark=" + selectedBenchmark
                + ", Treatment=" + treatment
                + ", Outcome=" + outcome
                + ", Model=" + model;
    }
}
