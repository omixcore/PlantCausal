package org.plantcausal.causal;

/**
 * Regression output including inference statistics.
 */
public record RegressionResult(
        double[] coefficients,
        double[] standardErrors,
        double[] tStatistics,
        double[] pValues,
        double[] lower95,
        double[] upper95,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using=org.plantcausal.report.json.FiniteDoubleSerializer.class) double rSquared
 ) {
    public RegressionResult {
        coefficients=coefficients.clone(); standardErrors=standardErrors.clone(); tStatistics=tStatistics.clone();
        pValues=pValues.clone(); lower95=lower95.clone(); upper95=upper95.clone();
    }
    @Override public double[] coefficients(){return coefficients.clone();}
    @Override public double[] standardErrors(){return standardErrors.clone();}
    @Override public double[] tStatistics(){return tStatistics.clone();}
    @Override public double[] pValues(){return pValues.clone();}
    @Override public double[] lower95(){return lower95.clone();}
    @Override public double[] upper95(){return upper95.clone();}
}
