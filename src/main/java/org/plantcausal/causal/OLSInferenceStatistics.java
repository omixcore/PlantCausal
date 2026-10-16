package org.plantcausal.causal;

public class OLSInferenceStatistics {
    public double[] standardErrors(double[][] covariance) {
        double[] se = new double[covariance.length];
        for (int i=0;i<covariance.length;i++) {
            se[i] = Math.sqrt(covariance[i][i]);
        }
        return se;
    }

    public double[] tStatistics(double[] beta, double[] se) {
        double[] t = new double[beta.length];
        for (int i=0;i<beta.length;i++) {
            t[i] = new OLSStatisticalInferenceEngine().tStatistic(beta[i],se[i]);
        }
        return t;
    }
}
