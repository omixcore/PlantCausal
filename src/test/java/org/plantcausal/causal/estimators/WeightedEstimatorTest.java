package org.plantcausal.causal.estimators;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WeightedEstimatorTest {
 @Test void balancedIpwAndAipwRecoverKnownEffect() {
  int n=120;double[][] x=new double[n][2];int[] t=new int[n];double[] y=new double[n];boolean[] r=new boolean[n];String[] g=new String[n];
  for(int i=0;i<n;i++){x[i][0]=1;x[i][1]=(i/4)%10-4.5;t[i]=(i/2)%2;y[i]=4+2*t[i]+.5*x[i][1]+(i%2==0?-.25:.25);r[i]=true;g[i]="G"+i;}
  for(var method:WeightedCausalEstimator.Method.values()) {
   var result=new WeightedCausalEstimator().estimate(x,t,y,r,g,method,false,200,123,.001);
   assertEquals(2,result.effect(),1e-8);assertTrue(result.low95()<2);assertTrue(result.high95()>2);assertEquals(120,result.propensity().size());
   assertEquals(120,result.effectiveSampleSize(),1e-6);
  }
 }
 @Test void separationIsRejected() {
  assertThrows(IllegalArgumentException.class,()->LogisticModel.fit(new double[][]{{1,-3},{1,-2},{1,-1},{1,1},{1,2},{1,3}},new int[]{0,0,0,1,1,1}));
 }
 @Test void interceptOnlyMatchesClassProportion() {
  var model=LogisticModel.fit(new double[][]{{1},{1},{1},{1},{1}},new int[]{0,0,1,1,1});assertEquals(.6,model.predict(new double[]{1}),1e-10);
 }
 @Test void cr1MatchesIndependentReference() {
  double[][] x={{1,0},{1,1},{1,0},{1,1},{1,0},{1,1},{1,0},{1,1}};
  double[] y={1,4,2,4,0,2,3,6};String[] g={"1","1","2","2","3","3","4","4"};
  var result=new ClusterRobustOLS().fit(x,y,g);assertEquals(2.5,result.coefficients()[1],1e-10);
  // statsmodels OLS.get_robustcov_results(cov_type='cluster', use_correction=True).
  assertEquals(0.3118047822311617,result.standardErrors()[1],1e-10);
 }
}
