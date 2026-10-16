package org.plantcausal.dataset;
import java.util.*;
/** Joins benchmark tables using sample identifiers, rejecting ambiguous joins. */
public class PlantCausalDatasetBuilder {
 public PlantCausalDataset build(List<Map<String,String>> phenotype,List<Map<String,String>> genotype,List<Map<String,String>> environment) {
  var ph=index(phenotype); var ge=index(genotype); var en=index(environment);
  if(!ph.keySet().equals(ge.keySet()) || !ph.keySet().equals(en.keySet())) throw new IllegalArgumentException("Sample identifiers differ across tables");
  return new PlantCausalDataset(List.copyOf(ph.keySet()),ph,ge,en,"treatment","biomass");
 }
 private Map<String,Map<String,String>> index(List<Map<String,String>> rows) {
  Map<String,Map<String,String>> result=new LinkedHashMap<>();
  for(var row:rows) {
   String id=row.get("sample");
   if(id==null || id.isBlank() || result.putIfAbsent(id,Map.copyOf(row))!=null) throw new IllegalArgumentException("Missing or duplicate sample identifier");
  }
  if(result.isEmpty()) throw new IllegalArgumentException("Empty benchmark table");
  return Collections.unmodifiableMap(result);
 }
}
