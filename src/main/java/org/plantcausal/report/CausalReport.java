package org.plantcausal.report;

import java.util.LinkedHashMap;
import java.util.Map;

public class CausalReport {

    private String benchmarkId;
    private String model;
    private final Map<String, Double> statistics = new LinkedHashMap<>();

    public void setBenchmarkId(String benchmarkId) {
        this.benchmarkId = benchmarkId;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void addStatistic(String name, double value) {
        statistics.put(name, value);
    }

    public String getBenchmarkId() {
        return benchmarkId;
    }

    public String getModel() {
        return model;
    }

    public Map<String, Double> getStatistics() {
        return statistics;
    }
}
