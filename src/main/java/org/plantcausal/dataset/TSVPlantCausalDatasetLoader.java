package org.plantcausal.dataset;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.commons.csv.*;
public class TSVPlantCausalDatasetLoader {
 public PlantCausalDataset load(Path path) throws IOException {
  return new PlantCausalDatasetBuilder().build(read(path.resolve("phenotype.tsv")),read(path.resolve("genotype.tsv")),read(path.resolve("environment.tsv")));
 }
 private List<Map<String,String>> read(Path file) throws IOException {
  try(var reader=Files.newBufferedReader(file); var parser=CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
   var headers=parser.getHeaderNames();
   if(!headers.contains("sample") || new HashSet<>(headers).size()!=headers.size()) throw new IOException("Invalid headers: "+file);
   List<Map<String,String>> rows=new ArrayList<>();
   for(var record:parser) {
    if(!record.isConsistent()) throw new IOException("Inconsistent TSV row: "+file+" row "+record.getRecordNumber());
    rows.add(record.toMap());
   }
   return rows;
  }
 }
}
