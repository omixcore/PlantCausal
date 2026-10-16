package org.plantcausal.gui;

import org.plantcausal.causal.CausalResult;

/**
 * Presentation layer for causal analysis reports.
 */
public class CausalReportViewer {

    public String renderSummary(CausalResult result) {
        if (result == null) {
            return "No causal result available.";
        }
        return "Causal Analysis Report\n" + result.toString();
    }
}
