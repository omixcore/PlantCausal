package org.plantcausal.causal;

public class CausalResult {

    private double averageTreatmentEffect;
    private double standardError;
    private double confidenceLow;
    private double confidenceHigh;
    private double pValue;
    private int sampleSize;
    private String diagnostics;

    public double getAverageTreatmentEffect(){ return averageTreatmentEffect; }
    public void setAverageTreatmentEffect(double value){ this.averageTreatmentEffect=value; }

    public double getStandardError(){ return standardError; }
    public void setStandardError(double value){ this.standardError=value; }

    public double getConfidenceLow(){ return confidenceLow; }
    public void setConfidenceLow(double value){ this.confidenceLow=value; }

    public double getConfidenceHigh(){ return confidenceHigh; }
    public void setConfidenceHigh(double value){ this.confidenceHigh=value; }

    public double getPValue(){ return pValue; }
    public void setPValue(double value){ this.pValue=value; }

    public int getSampleSize(){ return sampleSize; }
    public void setSampleSize(int value){ this.sampleSize=value; }

    public String getDiagnostics(){ return diagnostics; }
    public void setDiagnostics(String diagnostics){ this.diagnostics=diagnostics; }
}
