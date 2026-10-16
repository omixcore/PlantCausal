package org.plantcausal.report;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight exporter model for causal analysis reports.
 */
public class JsonCausalReportExporter {

    public Map<String, Object> exportSummary(CausalReport report) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("benchmark", report.getBenchmarkId());
        result.put("model", report.getModel());
        result.put("statistics", report.getStatistics());
        return result;
    }
}
