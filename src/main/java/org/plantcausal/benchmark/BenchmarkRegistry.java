package org.plantcausal.benchmark;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkRegistry {

    private final List<BenchmarkRecord> records = new ArrayList<>();

    public void register(BenchmarkRecord record){
        records.add(record);
    }

    public List<BenchmarkRecord> list(){
        return records;
    }
}
