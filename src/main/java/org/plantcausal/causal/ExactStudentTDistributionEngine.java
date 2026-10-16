package org.plantcausal.causal;
public class ExactStudentTDistributionEngine {
 public double cdf(double t,double df) {double p=twoSidedPValue(t,df);return t<0?p/2:1-p/2;}
 public double twoSidedPValue(double t,double df) {
  if(!(df>0) || !Double.isFinite(df) || Double.isNaN(t)) throw new IllegalArgumentException("Invalid t parameters");
  return org.plantcausal.math.RegularizedIncompleteBeta.evaluate(df/(df+t*t),df/2,0.5);
 }
}
