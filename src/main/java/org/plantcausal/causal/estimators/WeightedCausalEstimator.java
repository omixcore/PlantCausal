package org.plantcausal.causal.estimators;
import java.util.*;
import org.plantcausal.causal.*;
import org.apache.commons.math3.distribution.NormalDistribution;
/** Baseline binary-treatment ATE with optional outcome-observation weights; cluster bootstrap refits all nuisances. */
public final class WeightedCausalEstimator {
 public enum Method {IPW,AIPW}
 public record Estimate(double effect,double standardError,double p,double low95,double high95,
   List<Double> propensity,List<Double> observationProbability,List<Double> weights,
   double effectiveSampleSize,int bootstrapReplicates,long seed,Map<String,Object> nuisanceModels,List<Double> bootstrapDraws) {}
 private record Point(double effect,double[] propensity,double[] observation,double[] weights,Map<String,Object> models) {}
 public Estimate estimate(double[][] covariates,int[] treatment,double[] y,boolean[] observed,String[] cluster,
                          Method method,boolean observationWeighting,int repetitions,long seed,double overlap) {
  int n=covariates.length;
  if(n!=treatment.length||n!=y.length||n!=observed.length||n!=cluster.length||n<10)throw new IllegalArgumentException("Causal estimator dimensions/n are invalid");
  if(repetitions<200||repetitions>5000||!Double.isFinite(overlap)||overlap<=0||overlap>=.5)throw new IllegalArgumentException("Require 200..5000 bootstrap replicates and overlap in (0,.5)");
  Map<String,List<Integer>> blocks=new LinkedHashMap<>();
  for(int i=0;i<n;i++){
   if(cluster[i]==null||cluster[i].isBlank()||cluster[i].equals("NA"))throw new IllegalArgumentException("Documented cluster ID required");
   if(observed[i]&&!Double.isFinite(y[i]))throw new IllegalArgumentException("Invalid observed outcome");
   blocks.computeIfAbsent(cluster[i],k->new ArrayList<>()).add(i);
  }
  if(blocks.size()<10)throw new IllegalArgumentException("At least 10 independent resampling clusters required");
  var base=point(covariates,treatment,y,observed,method,observationWeighting,overlap);
  var groups=new ArrayList<>(blocks.values());Random random=new Random(seed);double[] draws=new double[repetitions];
  for(int b=0;b<repetitions;b++){
   List<Integer> indices=new ArrayList<>();for(int g=0;g<groups.size();g++)indices.addAll(groups.get(random.nextInt(groups.size())));
   int m=indices.size();double[][] c=new double[m][];int[] a=new int[m];double[] outcome=new double[m];boolean[] r=new boolean[m];
   for(int i=0;i<m;i++){int j=indices.get(i);c[i]=covariates[j];a[i]=treatment[j];outcome[i]=y[j];r[i]=observed[j];}
   try{draws[b]=point(c,a,outcome,r,method,observationWeighting,overlap).effect;}
   catch(IllegalArgumentException e){throw new IllegalArgumentException("Bootstrap replicate "+b+" failed; no failed draws are silently discarded. Increase data/support or revise nuisance models: "+e.getMessage(),e);}
  }
  double mean=Arrays.stream(draws).average().orElseThrow(),sum=0;for(double draw:draws)sum+=(draw-mean)*(draw-mean);
  double se=Math.sqrt(sum/(repetitions-1));if(!(se>0)||!Double.isFinite(se))throw new IllegalArgumentException("Degenerate bootstrap distribution");
  double pv=2*new NormalDistribution().cumulativeProbability(-Math.abs(base.effect/se));
  List<Double> originalDraws=list(draws);Arrays.sort(draws);double sw=0,sw2=0;for(double w:base.weights){sw+=w;sw2+=w*w;}
  return new Estimate(base.effect,se,pv,quantile(draws,.025),quantile(draws,.975),list(base.propensity),list(base.observation),list(base.weights),sw*sw/sw2,repetitions,seed,base.models,originalDraws);
 }
 private Point point(double[][] c,int[] a,double[] y,boolean[] observed,Method method,boolean ipcw,double overlap) {
  int n=c.length;LogisticModel ps=LogisticModel.fit(c,a);double[] e=new double[n],pi=new double[n],weights=new double[n];
  boolean all=true;int[] response=new int[n];for(int i=0;i<n;i++){response[i]=observed[i]?1:0;all&=observed[i];}
  double[][] responseDesign=new double[n][c[0].length+1];
  for(int i=0;i<n;i++){System.arraycopy(c[i],0,responseDesign[i],0,c[i].length);responseDesign[i][c[i].length]=a[i];}
  LogisticModel responseModel=!all&&ipcw?LogisticModel.fit(responseDesign,response):null;
  if(!all&&!ipcw)throw new IllegalArgumentException("Missing outcomes require explicit observation weighting or upstream complete-case selection");
  double sum1=0,sum0=0,w1=0,w0=0;
  for(int i=0;i<n;i++) {
   e[i]=ps.predict(c[i]);check(e[i],overlap,"Treatment positivity");pi[i]=responseModel==null?1:responseModel.predict(responseDesign[i]);
   if(responseModel!=null&&pi[i]<overlap)throw new IllegalArgumentException("Outcome-observation positivity failed");
   weights[i]=observed[i]?1/((a[i]==1?e[i]:1-e[i])*pi[i]):0;
   if(observed[i]) {if(a[i]==1){sum1+=weights[i]*y[i];w1+=weights[i];}else{sum0+=weights[i]*y[i];w0+=weights[i];}}
  }
  if(w1==0||w0==0)throw new IllegalArgumentException("No observed support in one treatment arm");
  Map<String,Object> models=new LinkedHashMap<>();models.put("propensityCoefficients",ps.coefficients());models.put("observationCoefficients",responseModel==null?List.of():responseModel.coefficients());
  if(method==Method.IPW)return new Point(sum1/w1-sum0/w0,e,pi,weights,Collections.unmodifiableMap(models));
  double[] b1=outcomeModel(c,a,y,observed,1),b0=outcomeModel(c,a,y,observed,0);double effect=0;
  for(int i=0;i<n;i++) {
   double m1=dot(c[i],b1),m0=dot(c[i],b0);effect+=m1-m0;
   if(observed[i])effect+=a[i]==1?(y[i]-m1)/(e[i]*pi[i]):-(y[i]-m0)/((1-e[i])*pi[i]);
  }
  models.put("outcomeArmA",list(b1));models.put("outcomeArmB",list(b0));
  return new Point(effect/n,e,pi,weights,Collections.unmodifiableMap(models));
 }
 private double[] outcomeModel(double[][] c,int[] a,double[] y,boolean[] r,int arm) {
  List<double[]> rows=new ArrayList<>();List<Double> values=new ArrayList<>();
  for(int i=0;i<c.length;i++)if(a[i]==arm&&r[i]){rows.add(c[i]);values.add(y[i]);}
  return new OLSStatisticalInferenceEngine().fit(rows.toArray(double[][]::new),values.stream().mapToDouble(Double::doubleValue).toArray()).coefficients();
 }
 private void check(double p,double threshold,String label){if(!Double.isFinite(p)||p<threshold||p>1-threshold)throw new IllegalArgumentException(label+" failed; probabilities are not silently clipped");}
 private static double dot(double[] x,double[] b){double sum=0;for(int i=0;i<x.length;i++)sum+=x[i]*b[i];return sum;}
 private static List<Double> list(double[] x){return Arrays.stream(x).boxed().toList();}
 private static double quantile(double[] x,double p){double index=(x.length-1)*p;int low=(int)index;return x[low]+(index-low)*(x[Math.min(low+1,x.length-1)]-x[low]);}
}
