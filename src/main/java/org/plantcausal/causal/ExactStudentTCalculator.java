package org.plantcausal.causal;
public class ExactStudentTCalculator extends ExactStudentTDistribution {
 public double studentTCdf(double t,int df){return cdf(t,df);}
}
