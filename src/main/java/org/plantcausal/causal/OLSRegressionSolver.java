package org.plantcausal.causal;
public class OLSRegressionSolver {
 public double[] solve(double[][] x,double[] y){return new OLSStatisticalInferenceEngine().fit(x,y).coefficients();}
}
