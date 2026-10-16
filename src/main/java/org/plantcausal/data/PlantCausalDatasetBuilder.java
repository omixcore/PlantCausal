package org.plantcausal.data;

import java.util.HashMap;
import java.util.Map;

public class PlantCausalDatasetBuilder {

    private final Map<String, Object> tables = new HashMap<>();

    public PlantCausalDatasetBuilder addTable(String name, Object table){
        tables.put(name, table);
        return this;
    }

    public PlantCausalDataset build(String datasetId){
        PlantCausalDataset dataset = new PlantCausalDataset();
        dataset.setDatasetId(datasetId);
        dataset.setTables(tables);
        return dataset;
    }
}
