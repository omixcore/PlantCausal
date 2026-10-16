package org.plantcausal.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified data object passed from validation to causal analysis.
 */
public class PlantCausalDataset {

    private String datasetId;
    private Map<String, Object> tables = new HashMap<>();

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public Map<String, Object> getTables() {
        return tables;
    }

    public void setTables(Map<String, Object> tables) {
        this.tables = tables;
    }

    public void addTable(String name, Object table) {
        tables.put(name, table);
    }
}
