package org.plantcausal.causal.estimators;
import java.util.*;
import org.apache.commons.math3.linear.*;
import org.plantcausal.causal.*;
/** CR1 cluster sandwich with G/(G-1)*(n-1)/(n-p) and t(G-1) intervals. */
public final class ClusterRobustOLS {
 public RegressionResult fit(double[][] x,double[] y,String[] clusters) {
  var engine=new OLSStatisticalInferenceEngine();var base=engine.fit(x,y);int n=x.length,p=x[0].length;
  if(clusters.length!=n)throw new IllegalArgumentException("Cluster length mismatch");
  double[] residual=engine.residuals(x,y,base.coefficients());
  Map<String,double[]> scores=new LinkedHashMap<>();
  for(int i=0;i<n;i++){
   if(clusters[i]==null||clusters[i].equals("NA")||clusters[i].isBlank())throw new IllegalArgumentException("Documented cluster IDs required");
   var score=scores.computeIfAbsent(clusters[i],k->new double[p]);for(int j=0;j<p;j++)score[j]+=x[i][j]*residual[i];
  }
  int g=scores.size();if(g<3)throw new IllegalArgumentException("At least three independent clusters required for CR1");
  // Scale before pseudoinverse to retain stability across physical units.
  double[] norms=new double[p];double[][] z=new double[n][p];
  for(int j=0;j<p;j++){for(int i=0;i<n;i++)norms[j]=Math.hypot(norms[j],x[i][j]);for(int i=0;i<n;i++)z[i][j]=x[i][j]/norms[j];}
  double[][] pinv=new QRDecomposition(new Array2DRowRealMatrix(z),1e-12).getSolver().getInverse().getData();
  for(int j=0;j<p;j++)for(int i=0;i<n;i++)pinv[j][i]/=norms[j];
  var inverse=new Array2DRowRealMatrix(pinv).multiply(new Array2DRowRealMatrix(pinv).transpose());
  double[][] meat=new double[p][p];
  for(var score:scores.values())for(int j=0;j<p;j++)for(int k=0;k<p;k++)meat[j][k]+=score[j]*score[k];
  var covariance=inverse.multiply(new Array2DRowRealMatrix(meat)).multiply(inverse).scalarMultiply((double)g/(g-1)*(n-1)/(n-p));
  double[] beta=base.coefficients(),se=new double[p],t=new double[p],pv=new double[p],lo=new double[p],hi=new double[p];
  double critical=org.plantcausal.math.ExactStudentTDistribution.criticalValue95(g-1);
  for(int j=0;j<p;j++) {se[j]=Math.sqrt(Math.max(0,covariance.getEntry(j,j)));t[j]=engine.tStatistic(beta[j],se[j]);pv[j]=org.plantcausal.math.ExactStudentTDistribution.twoSidedP(t[j],g-1);lo[j]=beta[j]-critical*se[j];hi[j]=beta[j]+critical*se[j];}
  return new RegressionResult(beta,se,t,pv,lo,hi,base.rSquared());
 }
}
