package org.plantcausal.causal;

/**
 * Statistical inference layer for OLS causal estimation.
 * Provides residual, variance and confidence interval calculation framework.
 */
public class OLSStatisticalInference {

    public double[] residuals(double[][] x, double[] y, double[] beta) {
        double[] result = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            double fitted = 0.0;
            for (int j = 0; j < beta.length; j++) {
                fitted += x[i][j] * beta[j];
            }
            result[i] = y[i] - fitted;
        }
        return result;
    }

    public double rSquared(double[] y, double[] residuals) {
        double mean = 0.0;
        for (double v : y) mean += v;
        mean /= y.length;

        double sst = 0.0;
        double sse = 0.0;
        for (int i = 0; i < y.length; i++) {
            sst += Math.pow(y[i] - mean, 2);
            sse += Math.pow(residuals[i], 2);
        }
        return sst == 0 ? 0 : 1 - sse / sst;
    }
}
