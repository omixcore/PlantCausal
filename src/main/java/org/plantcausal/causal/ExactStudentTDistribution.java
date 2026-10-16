package org.plantcausal.causal;
/** Compatibility facade for numerical Student-t inference. */
public class ExactStudentTDistribution {
 public double cdf(double t,int df){double p=twoSidedPValue(t,df); return t<0?p/2:1-p/2;}
 public double twoSidedPValue(double t,int df){return org.plantcausal.math.ExactStudentTDistribution.twoSidedP(t,df);}
 public double criticalValue95(int df){return org.plantcausal.math.ExactStudentTDistribution.criticalValue95(df);}
}
