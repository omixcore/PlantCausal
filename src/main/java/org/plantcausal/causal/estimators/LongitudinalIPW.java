package org.plantcausal.causal.estimators;
import java.util.*;
import org.apache.commons.math3.distribution.NormalDistribution;
/** Discrete-time sustained all-1 versus all-0 regimes, sequential treatment and optional censoring weights. */
public final class LongitudinalIPW {
 public record Subject(String id,String cluster,double[] baseline,double[][] history,int[] treatment,boolean[] retained,double outcome) {}
 public record Estimate(double effect,double se,double p,double low,double high,List<Double> weights,
   int adherentA,int adherentB,double effectiveSampleSize,int bootstrapReplicates,long seed,
   List<List<Double>> propensityHistory,List<List<Double>> observationHistory,List<Map<String,Object>> nuisanceModels,List<Double> bootstrapDraws) {}
 private record Point(double effect,double[] weights,int a,int b,List<List<Double>> e,List<List<Double>> pr,List<Map<String,Object>> models) {}
 public Estimate estimate(List<Subject> subjects,int repeats,long seed,double overlap,boolean censoringWeights) {
  if(repeats<200||repeats>5000||!(overlap>0&&overlap<.5))throw new IllegalArgumentException("Invalid longitudinal inference parameters");
  Map<String,List<Subject>> clusters=new LinkedHashMap<>();
  for(var s:subjects){if(s.cluster()==null||s.cluster().equals("NA"))throw new IllegalArgumentException("Cluster IDs required");clusters.computeIfAbsent(s.cluster(),k->new ArrayList<>()).add(s);}
  if(clusters.size()<10)throw new IllegalArgumentException("At least 10 independent clusters required");
  Point base=point(subjects,overlap,censoringWeights);var groups=new ArrayList<>(clusters.values());Random random=new Random(seed);double[] draws=new double[repeats];
  for(int b=0;b<repeats;b++) {
   List<Subject> sample=new ArrayList<>();for(int g=0;g<groups.size();g++)sample.addAll(groups.get(random.nextInt(groups.size())));
   try{draws[b]=point(sample,overlap,censoringWeights).effect;}
   catch(IllegalArgumentException e){throw new IllegalArgumentException("Longitudinal bootstrap replicate "+b+" failed; no failed draws discarded: "+e.getMessage(),e);}
  }
  double mean=Arrays.stream(draws).average().orElseThrow(),variance=0;for(double v:draws)variance+=(v-mean)*(v-mean);double se=Math.sqrt(variance/(repeats-1));
  if(!(se>0)||!Double.isFinite(se))throw new IllegalArgumentException("Degenerate longitudinal bootstrap");
  var originalDraws=Arrays.stream(draws).boxed().toList();Arrays.sort(draws);double sw=0,sw2=0;for(double w:base.weights){sw+=w;sw2+=w*w;}
  return new Estimate(base.effect,se,2*new NormalDistribution().cumulativeProbability(-Math.abs(base.effect/se)),quantile(draws,.025),quantile(draws,.975),Arrays.stream(base.weights).boxed().toList(),base.a,base.b,sw*sw/sw2,repeats,seed,base.e,base.pr,base.models,originalDraws);
 }
 private Point point(List<Subject> subjects,double overlap,boolean ipcw) {
  if(subjects.isEmpty())throw new IllegalArgumentException("No eligible longitudinal subjects");
  int n=subjects.size(),steps=subjects.get(0).treatment().length;
  if(steps==0)throw new IllegalArgumentException("No decision steps");
  Double[][] eHistory=new Double[n][steps],prHistory=new Double[n][steps];List<Map<String,Object>> models=new ArrayList<>();
  for(var s:subjects){if(s.treatment().length!=steps||s.retained().length!=steps||s.history().length!=steps)throw new IllegalArgumentException("Longitudinal dimensions mismatch");}
  double[] weight=new double[n];Arrays.fill(weight,1);
  boolean[] risk=new boolean[n];Arrays.fill(risk,true);
  for(int k=0;k<steps;k++) {
   List<Integer> ids=new ArrayList<>();for(int i=0;i<n;i++)if(risk[i])ids.add(i);
   if(ids.size()<10)throw new IllegalArgumentException("Insufficient sequential risk set at step "+k);
   double[][] x=new double[ids.size()][],rx=new double[ids.size()][];int[] a=new int[ids.size()],r=new int[ids.size()];boolean allRetained=true;
   for(int j=0;j<ids.size();j++) {
    Subject s=subjects.get(ids.get(j));x[j]=historyDesign(s,k);a[j]=s.treatment()[k];r[j]=s.retained()[k]?1:0;allRetained&=s.retained()[k];
    rx[j]=Arrays.copyOf(x[j],x[j].length+1);rx[j][x[j].length]=a[j];
   }
   LogisticModel treatment=LogisticModel.fit(x,a),retention=!allRetained&&ipcw?LogisticModel.fit(rx,r):null;
   models.add(Map.of("step",k,"riskSetSize",ids.size(),"treatmentCoefficients",treatment.coefficients(),"observationCoefficients",retention==null?List.of():retention.coefficients()));
   if(!allRetained&&!ipcw)throw new IllegalArgumentException("Dropout requires explicit IPCW missing policy");
   for(int j=0;j<ids.size();j++) {
    int i=ids.get(j);double e=treatment.predict(x[j]),pr=retention==null?1:retention.predict(rx[j]);
    eHistory[i][k]=e;prHistory[i][k]=pr;
    if(e<overlap||e>1-overlap||pr<overlap)throw new IllegalArgumentException("Sequential positivity/observation support failed at step "+k);
    if(r[j]==0){risk[i]=false;weight[i]=0;}else {
     weight[i]/=(a[j]==1?e:1-e)*pr;
     if(!Double.isFinite(weight[i]))throw new IllegalArgumentException("Longitudinal weight overflow");
    }
   }
  }
  double swA=0,swB=0,yA=0,yB=0;int countA=0,countB=0;
  for(int i=0;i<n;i++) {
   Subject s=subjects.get(i);boolean allA=true,allB=true;
   for(int a:s.treatment()){allA&=a==1;allB&=a==0;}
   if(!risk[i]||(!allA&&!allB)){weight[i]=0;continue;}
   if(!Double.isFinite(s.outcome()))throw new IllegalArgumentException("Retained subject requires observed outcome");
   if(allA){swA+=weight[i];yA+=weight[i]*s.outcome();countA++;}else{swB+=weight[i];yB+=weight[i]*s.outcome();countB++;}
  }
  if(countA<2||countB<2)throw new IllegalArgumentException("Insufficient uncensored sustained-regime support in one arm");
  return new Point(yA/swA-yB/swB,weight,countA,countB,nullableRows(eHistory),nullableRows(prHistory),List.copyOf(models));
 }
 private double[] historyDesign(Subject s,int k) {
  int l=s.history()[0].length;double[] x=new double[1+s.baseline().length+l*(k+1)+k];int col=0;x[col++]=1;
  for(double v:s.baseline())x[col++]=v;
  for(int t=0;t<=k;t++)for(double v:s.history()[t]){if(!Double.isFinite(v))throw new IllegalArgumentException("Missing pre-treatment history");x[col++]=v;}
  for(int t=0;t<k;t++)x[col++]=s.treatment()[t];return x;
 }
 private static List<List<Double>> nullableRows(Double[][] values){List<List<Double>> result=new ArrayList<>();for(Double[] row:values)result.add(Collections.unmodifiableList(Arrays.asList(row.clone())));return List.copyOf(result);}
 private static double quantile(double[] x,double p){double i=(x.length-1)*p;int j=(int)i;return x[j]+(i-j)*(x[Math.min(j+1,x.length-1)]-x[j]);}
}
