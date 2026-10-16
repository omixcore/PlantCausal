package org.plantcausal.benchmark;

public class BenchmarkRecord {

    private String id;
    private String species;
    private String stressType;
    private String accession;

    public String getId(){ return id; }
    public void setId(String id){ this.id=id; }

    public String getSpecies(){ return species; }
    public void setSpecies(String species){ this.species=species; }

    public String getStressType(){ return stressType; }
    public void setStressType(String stressType){ this.stressType=stressType; }

    public String getAccession(){ return accession; }
    public void setAccession(String accession){ this.accession=accession; }
}
