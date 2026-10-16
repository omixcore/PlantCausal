package org.plantcausal.causal;
public class OLSMatrixSolver {
 public double[] solve(double[][] x,double[] y){return new OLSStatisticalInferenceEngine().fit(x,y).coefficients();}
}
