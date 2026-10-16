package org.plantcausal.causal;

public class CausalAnalysisConfig {

    private String treatment;
    private String outcome;
    private String[] covariates;

    public String getTreatment(){ return treatment; }
    public void setTreatment(String treatment){ this.treatment = treatment; }

    public String getOutcome(){ return outcome; }
    public void setOutcome(String outcome){ this.outcome = outcome; }

    public String[] getCovariates(){ return covariates; }
    public void setCovariates(String[] covariates){ this.covariates = covariates; }
}
