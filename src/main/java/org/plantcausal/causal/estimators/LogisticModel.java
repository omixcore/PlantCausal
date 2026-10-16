package org.plantcausal.causal.estimators;
import org.apache.commons.math3.linear.*;
/** Unpenalized logistic MLE with scaling, line search and explicit separation failure. */
public final class LogisticModel {
 private final double[] beta,scale;
 private LogisticModel(double[] beta,double[] scale){this.beta=beta;this.scale=scale;}
 public static LogisticModel fit(double[][] design,int[] response) {
  int n=design.length,p=design[0].length,ones=0;
  if(response.length!=n||n<=p)throw new IllegalArgumentException("Logistic regression requires n > p");
  double[][] x=new double[n][p];double[] scale=new double[p];
  for(int i=0;i<n;i++){if(response[i]!=0&&response[i]!=1)throw new IllegalArgumentException("Binary response required");ones+=response[i];}
  if(ones==0||ones==n)throw new IllegalArgumentException("Logistic response has only one class");
  for(int j=0;j<p;j++) {
   for(int i=0;i<n;i++){if(design[i].length!=p||!Double.isFinite(design[i][j]))throw new IllegalArgumentException("Invalid logistic design");scale[j]=Math.hypot(scale[j],design[i][j]);}
   if(scale[j]==0)throw new IllegalArgumentException("Zero logistic design column");
   for(int i=0;i<n;i++)x[i][j]=design[i][j]/scale[j];
  }
  var matrix=new Array2DRowRealMatrix(x,false);
  if(new SingularValueDecomposition(matrix).getConditionNumber()>1e10)throw new IllegalArgumentException("Singular logistic design");
  double[] b=new double[p];double ll=likelihood(x,response,b);
  for(int iteration=0;iteration<100;iteration++) {
   double[][] h=new double[p][p];double[] g=new double[p];
   for(int i=0;i<n;i++){
    double prob=sigmoid(dot(x[i],b)),w=prob*(1-prob);
    for(int j=0;j<p;j++){g[j]+=x[i][j]*(response[i]-prob);for(int k=0;k<p;k++)h[j][k]+=x[i][j]*w*x[i][k];}
   }
   var solver=new QRDecomposition(new Array2DRowRealMatrix(h),1e-14).getSolver();
   if(!solver.isNonSingular())throw new IllegalArgumentException("Logistic separation or singular information matrix");
   double[] step=solver.solve(new ArrayRealVector(g)).toArray();double rate=1;double[] trial=new double[p];
   while(rate>=1e-8){for(int j=0;j<p;j++)trial[j]=b[j]+rate*step[j];if(likelihood(x,response,trial)>=ll-1e-12)break;rate/=2;}
   if(rate<1e-8)throw new IllegalArgumentException("Logistic line search failed");
   double delta=0;for(int j=0;j<p;j++)delta=Math.max(delta,Math.abs(trial[j]-b[j]));
   b=trial.clone();ll=likelihood(x,response,b);
   double maxEta=0;for(double[] row:x)maxEta=Math.max(maxEta,Math.abs(dot(row,b)));
   if(maxEta>30)throw new IllegalArgumentException("Logistic separation/extreme predictions; revise model or overlap");
   if(delta<1e-8)return new LogisticModel(b,scale);
  }
  throw new IllegalArgumentException("Logistic MLE did not converge");
 }
 public java.util.List<Double> coefficients(){double[] result=new double[beta.length];for(int j=0;j<beta.length;j++)result[j]=beta[j]/scale[j];return java.util.Arrays.stream(result).boxed().toList();}
 public double predict(double[] row){double eta=0;for(int j=0;j<beta.length;j++)eta+=row[j]/scale[j]*beta[j];return sigmoid(eta);}
 private static double dot(double[] a,double[] b){double s=0;for(int j=0;j<a.length;j++)s+=a[j]*b[j];return s;}
 private static double sigmoid(double v){return v>=0?1/(1+Math.exp(-v)):Math.exp(v)/(1+Math.exp(v));}
 private static double likelihood(double[][] x,int[] y,double[] b){double ll=0;for(int i=0;i<x.length;i++){double e=dot(x[i],b);ll+=y[i]*e-(Math.max(e,0)+Math.log1p(Math.exp(-Math.abs(e))));}return ll;}
}
