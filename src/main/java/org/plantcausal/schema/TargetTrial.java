package org.plantcausal.schema;
import java.util.List;
/** Legacy form model; v1 data standard remains defined by protocol.schema.json. */
public class TargetTrial {
 private String trialId,species,intervention,dataSource;
 private Population population;
 private List<String> outcomes;
 public String getTrialId(){return trialId;} public void setTrialId(String v){trialId=v;}
 public String getSpecies(){return species;} public void setSpecies(String v){species=v;}
 public String getIntervention(){return intervention;} public void setIntervention(String v){intervention=v;}
 public String getDataSource(){return dataSource;} public void setDataSource(String v){dataSource=v;}
 public Population getPopulation(){return population;} public void setPopulation(Population v){population=v;}
 public List<String> getOutcomes(){return outcomes;} public void setOutcomes(List<String> v){outcomes=v;}
 public static class Population {
  private int sampleSize;
  public int getSampleSize(){return sampleSize;} public void setSampleSize(int v){sampleSize=v;}
 }
}
