package org.plantcausal.causal;
/** Compatibility report facade; treatment column is column 1 after the intercept. */
public class OLSStatisticalEngine {
 public OLSStatisticalReport fit(double[][] x,double[] y) {
  var r=new OLSStatisticalInferenceEngine().fit(x,y); int j=x[0].length>1?1:0;
  var report=new OLSStatisticalReport();report.setCoefficients(r.coefficients());report.setSampleSize(y.length);
  report.setCoefficient(r.coefficients()[j]);report.setStandardError(r.standardErrors()[j]);report.setTStatistic(r.tStatistics()[j]);report.setPValue(r.pValues()[j]);report.setRSquared(r.rSquared());
  report.setDiagnostics("OLS fitted; causal identification not assessed");return report;
 }
}
