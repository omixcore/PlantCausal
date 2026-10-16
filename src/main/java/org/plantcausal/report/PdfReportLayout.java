package org.plantcausal.report;

/**
 * Defines the PDF report layout model.
 *
 * Sections:
 * - benchmark metadata
 * - target trial definition
 * - DAG summary
 * - causal effect estimation
 * - statistical diagnostics
 */
public class PdfReportLayout {

    public String title = "PlantCausal Causal Analysis Report";

    public String[] sections() {
        return new String[]{
                "Benchmark Metadata",
                "Target Trial Definition",
                "Causal Graph",
                "Effect Estimate",
                "Statistical Diagnostics"
        };
    }
}
