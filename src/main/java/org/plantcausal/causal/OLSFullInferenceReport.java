package org.plantcausal.causal;

public class OLSFullInferenceReport {
    private final double[] coefficients;
    private final double[] standardErrors;
    private final double[] tStatistics;
    private final double[] pValues;
    private final double rSquared;
    private final int sampleSize;

    public OLSFullInferenceReport(double[] coefficients,
                                  double[] standardErrors,
                                  double[] tStatistics,
                                  double[] pValues,
                                  double rSquared,
                                  int sampleSize) {
        this.coefficients = coefficients;
        this.standardErrors = standardErrors;
        this.tStatistics = tStatistics;
        this.pValues = pValues;
        this.rSquared = rSquared;
        this.sampleSize = sampleSize;
    }

    public double[] getCoefficients() { return coefficients; }
    public double[] getStandardErrors() { return standardErrors; }
    public double[] getTStatistics() { return tStatistics; }
    public double[] getPValues() { return pValues; }
    public double getRSquared() { return rSquared; }
    public int getSampleSize() { return sampleSize; }
}
