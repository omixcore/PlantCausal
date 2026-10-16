package org.plantcausal.math;
/** Validated regularized incomplete beta, using the Apache Commons Math backend. */
public class RegularizedIncompleteBeta {
 public static double evaluate(double x,double a,double b) {
  if(!Double.isFinite(x)||x<0||x>1||!Double.isFinite(a)||!Double.isFinite(b)||a<=0||b<=0) throw new IllegalArgumentException("Invalid beta parameters");
  return org.apache.commons.math3.special.Beta.regularizedBeta(x,a,b,1e-14,10000);
 }
}
