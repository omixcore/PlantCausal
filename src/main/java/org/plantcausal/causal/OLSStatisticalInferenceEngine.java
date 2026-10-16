package org.plantcausal.causal;

/**
 * OLS statistical inference layer.
 * Produces RegressionResult from fitted coefficients.
 */
public class OLSStatisticalInferenceEngine {

    public RegressionResult fit(double[][] x, double[] y) {
        validate(x,y);
        int p=x[0].length;
        double[][] scaled=new double[x.length][p]; double[] norm=new double[p];
        for(int j=0;j<p;j++) {
            for(int i=0;i<x.length;i++) norm[j]=Math.hypot(norm[j],x[i][j]);
            if(norm[j]==0 || !Double.isFinite(norm[j])) throw new IllegalArgumentException("Zero or overflowing design column");
            for(int i=0;i<x.length;i++) scaled[i][j]=x[i][j]/norm[j];
        }
        var matrix=new org.apache.commons.math3.linear.Array2DRowRealMatrix(scaled,false);
        if(new org.apache.commons.math3.linear.SingularValueDecomposition(matrix).getConditionNumber()>1e10)
            throw new IllegalArgumentException("Singular or ill-conditioned design");
        var solver=new org.apache.commons.math3.linear.QRDecomposition(matrix,1e-12).getSolver();
        if(!solver.isNonSingular()) throw new IllegalArgumentException("Singular design");
        double[] beta=solver.solve(new org.apache.commons.math3.linear.ArrayRealVector(y)).toArray();
        double[][] pinv=solver.getInverse().getData();
        for(int j=0;j<p;j++) { beta[j]/=norm[j]; for(int i=0;i<x.length;i++) pinv[j][i]/=norm[j]; }
        double[][] inv=new double[p][p];
        for(int j=0;j<p;j++) for(int k=0;k<p;k++) for(int i=0;i<x.length;i++) inv[j][k]+=pinv[j][i]*pinv[k][i];
        return infer(x,y,beta,inv);
    }
    private void validate(double[][] x,double[] y) {
        if(x==null || y==null || x.length==0 || x.length!=y.length || x[0]==null
           || x[0].length==0 || x.length<=x[0].length) throw new IllegalArgumentException("OLS requires n > p and matching nonempty X/y");
        for(int i=0;i<x.length;i++) {
            if(x[i]==null || x[i].length!=x[0].length || !Double.isFinite(y[i])) throw new IllegalArgumentException("Invalid design/response row");
            for(double v:x[i]) if(!Double.isFinite(v)) throw new IllegalArgumentException("Non-finite design");
        }
    }
    public RegressionResult infer(double[][] x, double[] y, double[] beta, double[][] xtxInverse) {
        validate(x,y);
        if(beta==null || xtxInverse==null || beta.length!=x[0].length || xtxInverse.length!=beta.length) throw new IllegalArgumentException("Inference dimensions mismatch");
        for(double b:beta) if(!Double.isFinite(b)) throw new IllegalArgumentException("Non-finite fitted coefficient; rescale inputs");
        for(double[] row:xtxInverse) {
            if(row==null || row.length!=beta.length) throw new IllegalArgumentException("Invalid inverse dimensions");
            for(double v:row) if(!Double.isFinite(v)) throw new IllegalArgumentException("Non-finite coefficient covariance");
        }
        double[] residuals = residuals(x, y, beta);
        double variance = residualVariance(x, residuals);
        double[][] covariance = covarianceMatrix(xtxInverse, variance);
        double[] se = standardErrors(covariance);
        double[] t = new double[beta.length];
        double[] p = new double[beta.length];
        double[] lower = new double[beta.length], upper = new double[beta.length];
        StudentTInferenceEngine engine = new StudentTInferenceEngine();
        int df = x.length - x[0].length;
        double critical = org.plantcausal.math.ExactStudentTDistribution.criticalValue95(df);
        for (int i = 0; i < beta.length; i++) {
            t[i] = tStatistic(beta[i], se[i]);
            p[i] = engine.twoSidedPValue(t[i], df);
            double[] ci = confidenceInterval(beta[i], se[i], critical);
            lower[i]=ci[0]; upper[i]=ci[1];
        }
        return new RegressionResult(beta, se, t, p, lower, upper, rSquared(y, residuals));
    }

    public double[] residuals(double[][] x, double[] y, double[] beta) {
        double[] result = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            for (int j = 0; j < beta.length; j++) result[i] += x[i][j] * beta[j];
            result[i] = y[i] - result[i];
        }
        return result;
    }

    public double residualVariance(double[][] x, double[] residuals) {
        double sse = 0;
        for (double r : residuals) sse += r*r;
        if(!Double.isFinite(sse)) throw new IllegalArgumentException("Residual sum of squares overflow; rescale response");
        if(x.length<=x[0].length) throw new IllegalArgumentException("No residual degrees of freedom");
        return sse / (x.length - x[0].length);
    }

    public double[][] covarianceMatrix(double[][] inv, double variance) {
        double[][] c = new double[inv.length][inv.length];
        for (int i=0;i<inv.length;i++) for(int j=0;j<inv.length;j++) c[i][j]=inv[i][j]*variance;
        return c;
    }

    public double[] standardErrors(double[][] c) {
        double[] s=new double[c.length];
        for(int i=0;i<c.length;i++) {
            if(c[i][i]<0 || !Double.isFinite(c[i][i])) throw new IllegalArgumentException("Invalid coefficient variance");
            s[i]=Math.sqrt(c[i][i]);
        }
        return s;
    }

    public double tStatistic(double b,double se){return se==0?(b==0?0:Math.copySign(Double.POSITIVE_INFINITY,b)):b/se;}

    public double[] confidenceInterval(double b,double se,double critical){return new double[]{b-critical*se,b+critical*se};}

    public double rSquared(double[] y,double[] residuals){
        double mean=0; for(double v:y) mean+=v; mean/=y.length;
        double sst=0,sse=0; for(int i=0;i<y.length;i++){sst+=(y[i]-mean)*(y[i]-mean);sse+=residuals[i]*residuals[i];}
        return sst==0?Double.NaN:1.0-sse/sst;
    }
}
