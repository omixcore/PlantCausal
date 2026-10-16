package org.plantcausal.causal;

/**
 * High-level causal regression workflow connecting dataset matrices,
 * OLS estimation and statistical inference.
 */
public class CausalRegressionEngine {

    private final OLSMatrixSolver solver;

    public CausalRegressionEngine() {
        this.solver = new OLSMatrixSolver();
    }

    public RegressionResult estimate(double[][] designMatrix, double[] outcome) {
        double[] coefficients = solver.solve(designMatrix, outcome);
        return new RegressionResult(coefficients);
    }

    public record RegressionResult(double[] coefficients) {}
}
