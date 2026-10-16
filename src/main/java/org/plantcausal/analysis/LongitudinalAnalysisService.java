package org.plantcausal.analysis;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.*;
import org.plantcausal.core.*;
import org.plantcausal.core.Models.*;
import org.plantcausal.desktop.WorkspaceService;
import org.plantcausal.causal.*;
import org.plantcausal.causal.estimators.LongitudinalIPW;
/** Adapts schema-v1 histories to discrete-time sustained-regime IPW. */
public final class LongitudinalAnalysisService {
 public AnalysisService.Result analyze(Path directory,DataPackage d,AnalysisService.Options options,String fingerprint) throws Exception {
  TargetTrial t=d.protocol().targetTrial();
  if(!t.estimand().effect().equals("mean_difference")||!t.estimand().strategyEffect().equals("sustained_strategy"))throw new IllegalArgumentException("MSM_IPW requires a sustained_strategy mean_difference estimand");
  if(options.missingPolicy()==AnalysisService.MissingPolicy.COMPLETE_CASE)throw new IllegalArgumentException("Longitudinal complete-case filtering is unsupported; choose FAIL or IPCW");
  if(options.adjustGenotype()||options.adjustBlock())throw new IllegalArgumentException("For longitudinal nuisance models encode prespecified baseline genotype/block indicators as numeric baseline variables");
  var gridNode=d.protocol().document().path("extensions").path("longitudinal").path("time_grid");
  if(!gridNode.isArray()||gridNode.size()<2)throw new IllegalArgumentException("Define extensions.longitudinal.time_grid, including time zero and primary endpoint");
  List<BigDecimal> grid=new ArrayList<>();for(var value:gridNode){if(!value.isNumber())throw new IllegalArgumentException("Numeric time grid required");grid.add(value.decimalValue());}
  if(grid.get(0).signum()!=0||grid.get(grid.size()-1).compareTo(t.outcome().time())!=0)throw new IllegalArgumentException("Time grid must start at zero and end at the primary outcome");
  for(int i=1;i<grid.size();i++)if(grid.get(i).compareTo(grid.get(i-1))<=0)throw new IllegalArgumentException("Strictly increasing time grid required");
  var endpointDefinition=definition(d,t.outcome().variableId());
  if(!Set.of("number","integer").contains(endpointDefinition.dataType()))throw new IllegalArgumentException("Numeric continuous endpoint required");
  int steps=grid.size()-1;String[] contrast=t.estimand().contrast().split("-");
  for(String id:options.baselineCovariates()) {
   var v=definition(d,id);if(!Set.of("baseline_confounder","precision_covariate").contains(v.role())||!Set.of("number","integer").contains(v.dataType()))throw new IllegalArgumentException("Invalid numeric baseline covariate: "+id);
  }
  for(String id:options.timeVaryingCovariates()){
   var v=definition(d,id);if(!v.role().equals("time_varying_confounder")||!Set.of("number","integer").contains(v.dataType()))throw new IllegalArgumentException("Declare numeric time_varying_confounder: "+id);
  }
  List<LongitudinalIPW.Subject> subjects=new ArrayList<>();List<String> ids=new ArrayList<>();Map<String,String> allocationClusters=new HashMap<>();
  for(var unit:d.units())if(unit.eligible()&&unit.unitType().equals(t.analysisUnit())) {
   if(unit.clusterId().equals("NA"))throw new IllegalArgumentException("Documented independent inference clusters required");
   String previous=allocationClusters.putIfAbsent(unit.assignmentUnitId(),unit.clusterId());
   if(previous!=null&&!previous.equals(unit.clusterId()))throw new IllegalArgumentException("A shared assignment unit cannot be split across inference clusters");
   double[] baseline=new double[options.baselineCovariates().size()];
   for(int j=0;j<baseline.length;j++)baseline[j]=measurement(d,unit.unitId(),options.baselineCovariates().get(j),BigDecimal.ZERO,null);
   double[][] history=new double[steps][options.timeVaryingCovariates().size()];int[] a=new int[steps];boolean[] retained=new boolean[steps];
   var losses=d.events().stream().filter(e->e.unitId().equals(unit.unitId())&&e.eventType().equals("loss_to_followup")).toList();
   if(losses.size()>1)throw new IllegalArgumentException("Multiple loss events require reconciliation");
   BigDecimal loss=losses.isEmpty()?null:losses.get(0).time();
   if(loss!=null&&grid.stream().noneMatch(v->v.compareTo(loss)==0))throw new IllegalArgumentException("This discrete-time estimator requires loss events on grid boundaries");
   if(loss!=null&&loss.signum()==0)throw new IllegalArgumentException("Loss at time zero has no treatment support");
   var endpoints=d.observations().stream().filter(o->o.unitId().equals(unit.unitId())&&o.variableId().equals(t.outcome().variableId())&&o.time().compareTo(t.outcome().time())==0).toList();
   if(endpoints.size()!=1)throw new IllegalArgumentException("Exactly one endpoint row required");
   boolean endpointObserved=!endpoints.get(0).value().equals("NA");
   if(!endpointObserved&&d.events().stream().anyMatch(e->e.unitId().equals(unit.unitId())&&e.eventType().equals("death")&&e.time().compareTo(t.outcome().time())<=0))throw new IllegalArgumentException("Outcome truncated by death requires a defined composite/principal-stratum estimand, not automatic IPCW");
   if(loss!=null&&loss.compareTo(t.outcome().time())<=0&&endpointObserved)throw new IllegalArgumentException("Observed primary endpoint contradicts loss before endpoint; reconcile source history");
   boolean risk=true;
   for(int k=0;k<steps;k++) {
    if(!risk)continue;
    final int step=k;
    var intervals=d.treatments().stream().filter(v->v.assignmentUnitId().equals(unit.assignmentUnitId())&&v.start().compareTo(grid.get(step))==0&&v.stop().compareTo(grid.get(step+1))==0).toList();
    if(intervals.size()!=1)throw new IllegalArgumentException("Exactly one actual strategy interval per at-risk unit/grid step required: "+unit.unitId()+":"+k);
    var interval=intervals.get(0);if(interval.adherence().equals("unknown"))throw new IllegalArgumentException("Unverified actual strategy/adherence history cannot identify sustained exposure");
    if(!interval.strategyId().equals(contrast[0])&&!interval.strategyId().equals(contrast[1]))throw new IllegalArgumentException("Unsupported strategy in actual history");
    a[k]=interval.strategyId().equals(contrast[0])?1:0;
    if(k==0&&!interval.strategyId().equals(unit.strategyId()))throw new IllegalArgumentException("Baseline strategy differs from first actual strategy");
    for(int j=0;j<history[k].length;j++)history[k][j]=measurement(d,unit.unitId(),options.timeVaryingCovariates().get(j),grid.get(k),k==0?null:grid.get(k-1));
    retained[k]=(loss==null||loss.compareTo(grid.get(k+1))>0)&&(k!=steps-1||endpointObserved);risk=retained[k];
   }
   double outcome=endpointObserved?Double.parseDouble(endpoints.get(0).value()):Double.NaN;
   subjects.add(new LongitudinalIPW.Subject(unit.unitId(),unit.clusterId(),baseline,history,a,retained,outcome));ids.add(unit.unitId());
  }
  var estimate=new LongitudinalIPW().estimate(subjects,options.bootstrapReplicates(),options.seed(),options.overlapThreshold(),options.missingPolicy()==AnalysisService.MissingPolicy.IPCW);
  var regression=new RegressionResult(new double[]{estimate.effect()},new double[]{estimate.se()},new double[]{estimate.effect()/estimate.se()},new double[]{estimate.p()},new double[]{estimate.low()},new double[]{estimate.high()},Double.NaN);
  Map<String,Object> details=new LinkedHashMap<>();details.put("estimator","MSM_IPW");long observedCount=subjects.stream().filter(s->Double.isFinite(s.outcome())).count();details.put("observedEndpoints",observedCount);details.put("missingEndpoints",subjects.size()-observedCount);details.put("dependencyVersions",Map.of("commonsMath","3.6.1","pdfBox","3.0.3"));details.put("timeGrid",List.copyOf(grid));details.put("propensityHistory",estimate.propensityHistory());details.put("observationHistory",estimate.observationHistory());List<Map<String,Object>> modelDescriptions=new ArrayList<>();
  for(int step=0;step<steps;step++) {
   Map<String,Object> model=new LinkedHashMap<>(estimate.nuisanceModels().get(step));
   List<String> columns=new ArrayList<>(List.of("Intercept"));columns.addAll(options.baselineCovariates());
   for(int j=0;j<=step;j++)for(String id:options.timeVaryingCovariates())columns.add(id+"[pre-decision "+grid.get(j)+"]");
   for(int j=0;j<step;j++)columns.add("ArmA[decision "+grid.get(j)+"]");
   model.put("treatmentTerms",List.copyOf(columns));columns.add("CurrentArmA");model.put("observationTerms",List.copyOf(columns));modelDescriptions.add(Collections.unmodifiableMap(model));
  }
  details.put("nuisanceModels",List.copyOf(modelDescriptions));details.put("bootstrapDraws",estimate.bootstrapDraws());details.put("weights",estimate.weights());details.put("effectiveSampleSize",estimate.effectiveSampleSize());details.put("sustainedArmA",estimate.adherentA());details.put("sustainedArmB",estimate.adherentB());details.put("inference","Cluster percentile bootstrap CI, all sequential nuisance models refitted; bootstrap-SE normal Wald p");
  if(!fingerprint.equals(new WorkspaceService().fingerprint(directory)))throw new IllegalArgumentException("Input changed during longitudinal analysis");
  int armA=(int)subjects.stream().filter(s->s.treatment()[0]==1).count();
  return new AnalysisService.Result("0.4.0",d.protocol().trialId(),fingerprint,d.protocol().dataKind(),t.outcome().variableId(),t.outcome().unit(),t.estimand().contrast(),d.protocol().dataKind().equals("synthetic")?"SYNTHETIC_DEMONSTRATION":"SUSTAINED_REGIME_ESTIMATE_REQUIRES_IDENTIFICATION_REVIEW","NOT_ASSESSED",options,List.copyOf(ids),List.of(contrast[0]+" at all steps minus "+contrast[1]+" at all steps"),regression,subjects.size(),0,armA,subjects.size()-armA,
   List.of("Requires sequential exchangeability, positivity, consistency, no interference, correct treatment and censoring models.","Only two static sustained regimes on the explicit discrete grid are supported; this is not a dynamic-policy or survival estimator.","Pre-treatment history is modeled before each decision. Missing histories are rejected, not imputed.","Unstabilized sequential weights; arm-normalized Hajek regime means. Bootstrap resamples whole inference clusters.","IPCW requires conditional independent censoring given observed history; death is not automatically treated as loss to follow-up."),List.of(),List.of(),Collections.unmodifiableMap(details));
 }
 private VariableDefinition definition(DataPackage d,String id){return d.variables().stream().filter(v->v.variableId().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown variable: "+id));}
 private double measurement(DataPackage d,String unit,String id,BigDecimal before,BigDecimal after) {
  var records=d.observations().stream().filter(o->o.unitId().equals(unit)&&o.variableId().equals(id)&&(o.time().compareTo(before)<0||(before.signum()==0&&o.time().signum()==0&&definition(d,id).timing().equals("before_first_treatment")))&&(after==null||o.time().compareTo(after)>0)).toList();
  if(records.size()!=1||records.get(0).value().equals("NA"))throw new IllegalArgumentException("Exactly one observed pre-decision measurement required: "+unit+":"+id+":"+before);
  double result=Double.parseDouble(records.get(0).value());if(!Double.isFinite(result))throw new IllegalArgumentException("Invalid history value");return result;
 }
}
