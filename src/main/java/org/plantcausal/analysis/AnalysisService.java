package org.plantcausal.analysis;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import org.plantcausal.core.*;
import org.plantcausal.core.Models.*;
import org.plantcausal.causal.*;
import org.plantcausal.desktop.WorkspaceService;

/** Fixed-endpoint additive OLS. Identification remains an explicit scientific review. */
public final class AnalysisService {
    public enum Estimator {OLS,CLUSTER_OLS,IPW,AIPW,MSM_IPW}
    public enum MissingPolicy {FAIL,COMPLETE_CASE,IPCW}
    public record Options(List<String> baselineCovariates, boolean adjustGenotype, boolean adjustBlock,
                          Estimator estimator,MissingPolicy missingPolicy,int bootstrapReplicates,long seed,double overlapThreshold,List<String> timeVaryingCovariates) {
        public Options(List<String> covariates,boolean genotype,boolean block,Estimator estimator,MissingPolicy missing,int repeats,long seed,double overlap) {
            this(covariates,genotype,block,estimator,missing,repeats,seed,overlap,List.of());
        }
        public Options(List<String> covariates,boolean genotype,boolean block) {
            this(covariates,genotype,block,Estimator.OLS,MissingPolicy.FAIL,300,20261013L,.01);
        }
        public Options {
            if(estimator==null||missingPolicy==null)throw new IllegalArgumentException("Estimator and missing policy required");
            if(missingPolicy==MissingPolicy.IPCW && estimator!=Estimator.IPW && estimator!=Estimator.AIPW && estimator!=Estimator.MSM_IPW)throw new IllegalArgumentException("IPCW requires IPW or AIPW");
            if(bootstrapReplicates<200||bootstrapReplicates>5000||!Double.isFinite(overlapThreshold)||overlapThreshold<=0||overlapThreshold>=.5)throw new IllegalArgumentException("Require 200..5000 bootstrap replicates and finite overlap in (0,.5)");
            timeVaryingCovariates=List.copyOf(timeVaryingCovariates);
            if(!timeVaryingCovariates.isEmpty()&&estimator!=Estimator.MSM_IPW)throw new IllegalArgumentException("Time-varying histories require MSM_IPW");
            if(new HashSet<>(timeVaryingCovariates).size()!=timeVaryingCovariates.size())throw new IllegalArgumentException("Duplicate time-varying covariates");
            baselineCovariates=List.copyOf(baselineCovariates);
            if(new HashSet<>(baselineCovariates).size()!=baselineCovariates.size()) throw new IllegalArgumentException("Duplicate covariates");
        }
        public static Options unadjusted() {return new Options(List.of(),false,false);}
    }
    public record Result(String softwareVersion,String trialId,String inputSha256,String dataKind,
            String outcome,String unit,String contrast,String interpretation,String identificationStatus,
            Options options,List<String> unitIds,List<String> terms,RegressionResult regression,
            int sampleSize,int residualDf,int armA,int armB,List<String> warnings,
            List<Double> fitted,List<Double> residuals,Map<String,Object> estimatorDiagnostics) {}

    public Result analyze(Path directory,Options options) throws Exception {
        WorkspaceService workspace=new WorkspaceService();
        var snapshot=workspace.validate(directory);
        if(!snapshot.report().schemaValid()) throw new IOException("Format validation failed: "+snapshot.report().issues().stream().filter(i->i.severity().equals("ERROR")).toList());
        PackageReader reader=new PackageReader();
        DataPackage d=reader.read(directory,reader.readProtocol(directory));
        TargetTrial t=d.protocol().targetTrial();
        if(options.estimator()==Estimator.MSM_IPW)return new LongitudinalAnalysisService().analyze(directory,d,options,snapshot.fingerprint());
        if(!t.estimand().effect().equals("mean_difference")) throw new IllegalArgumentException("Only continuous mean_difference is supported by this estimator");
        if(options.estimator()==Estimator.OLS && !t.assignmentUnit().equals(t.analysisUnit())) throw new IllegalArgumentException("Nested allocation requires cluster-aware inference; unsupported by independent-unit OLS");
        var selected=d.units().stream().filter(u->u.eligible() && u.unitType().equals(t.analysisUnit())).toList();
        String[] contrast=t.estimand().contrast().split("-",-1);
        Set<String> assignment=new HashSet<>(),clusters=new HashSet<>();Map<String,String> allocationClusters=new HashMap<>();
        for(var u:selected) {
            String inferenceCluster=allocationClusters.putIfAbsent(u.assignmentUnitId(),u.clusterId());
            if(inferenceCluster!=null&&!inferenceCluster.equals(u.clusterId()))throw new IllegalArgumentException("Shared allocation units must remain in one inference cluster");
            if(options.estimator()==Estimator.OLS && (!u.assignmentUnitId().equals(u.unitId()) || !assignment.add(u.assignmentUnitId()))) throw new IllegalArgumentException("Repeated allocation units require cluster-aware inference");
            if(u.clusterId().equals("NA") || (options.estimator()==Estimator.OLS && !clusters.add(u.clusterId()))) throw new IllegalArgumentException("Independent inference requires documented distinct cluster IDs");
            if(!u.strategyId().equals(contrast[0]) && !u.strategyId().equals(contrast[1])) throw new IllegalArgumentException("Additional eligible strategy arms require an explicitly restricted protocol");
        }
        List<String> terms=new ArrayList<>(List.of("Intercept",contrast[0]+" minus "+contrast[1]));
        Map<String,VariableDefinition> definitions=new HashMap<>();
        d.variables().forEach(v->definitions.put(v.variableId(),v));
        var outcome=definitions.get(t.outcome().variableId());
        if(!Set.of("number","integer").contains(outcome.dataType())) throw new IllegalArgumentException("OLS requires a numeric continuous endpoint");
        for(String id:options.baselineCovariates()) {
            var v=definitions.get(id);
            if(v==null || !Set.of("baseline_confounder","precision_covariate").contains(v.role()) || !Set.of("number","integer").contains(v.dataType()))
                throw new IllegalArgumentException("Covariate must be declared numeric baseline_confounder or precision_covariate: "+id);
            terms.add(id);
        }
        List<String> genotypes=categories(selected,true,options.adjustGenotype()), blocks=categories(selected,false,options.adjustBlock());
        for(int j=1;j<genotypes.size();j++) terms.add("Genotype "+genotypes.get(j)+" vs "+genotypes.get(0));
        for(int j=1;j<blocks.size();j++) terms.add("Block "+blocks.get(j)+" vs "+blocks.get(0));
        int eligibleCount=selected.size();
        if(options.missingPolicy()==MissingPolicy.COMPLETE_CASE) selected=selected.stream().filter(u->d.observations().stream().anyMatch(o->o.unitId().equals(u.unitId())&&o.variableId().equals(t.outcome().variableId())&&o.time().compareTo(t.outcome().time())==0&&!o.value().equals("NA"))).toList();
        double[][] x=new double[selected.size()][terms.size()]; double[] y=new double[selected.size()];
        int armA=0; boolean[] observed=new boolean[selected.size()];
        List<String> warnings=new ArrayList<>();
        warnings.add("Identification assumptions are not automatically verified. Format validity is not causal readiness.");
        if(!t.estimand().strategyEffect().equals("baseline_assignment")) warnings.add("This analysis contrasts baseline assigned strategies; it does not estimate adherence-dependent sustained-strategy or per-protocol effects.");
        if(!d.events().isEmpty()) warnings.add("Intercurrent events are retained in source records; this estimator does not apply censoring weights or competing-risk models.");
        for(int i=0;i<selected.size();i++) {
            var u=selected.get(i); x[i][0]=1; x[i][1]=u.strategyId().equals(contrast[0])?1:0; armA+=(int)x[i][1];
                        if(options.missingPolicy()==MissingPolicy.IPCW) {
                var endpoints=d.observations().stream().filter(o->o.unitId().equals(u.unitId())&&o.variableId().equals(t.outcome().variableId())&&o.time().compareTo(t.outcome().time())==0).toList();
                if(endpoints.size()!=1)throw new IllegalArgumentException("Exactly one endpoint record required");
                observed[i]=!endpoints.get(0).value().equals("NA");
                if(!observed[i]&&d.events().stream().anyMatch(e->e.unitId().equals(u.unitId())&&e.eventType().equals("death")&&e.time().compareTo(t.outcome().time())<=0))throw new IllegalArgumentException("Outcome truncated by death requires a defined estimand; it cannot be handled as ordinary MAR missingness");
                y[i]=observed[i]?value(d,u.unitId(),t.outcome().variableId(),t.outcome().time(),false,warnings):Double.NaN;
            } else {observed[i]=true;y[i]=value(d,u.unitId(),t.outcome().variableId(),t.outcome().time(),false,warnings);}
            int col=2;
            for(String id:options.baselineCovariates()) x[i][col++]=value(d,u.unitId(),id,null,true,warnings);
            for(int j=1;j<genotypes.size();j++) x[i][col++]=u.genotype().equals(genotypes.get(j))?1:0;
            for(int j=1;j<blocks.size();j++) x[i][col++]=u.blockId().equals(blocks.get(j))?1:0;
        }
        double minOutcome=java.util.Arrays.stream(y).min().orElse(Double.NaN),maxOutcome=java.util.Arrays.stream(y).max().orElse(Double.NaN);
        if(minOutcome==maxOutcome) throw new IllegalArgumentException("Endpoint has no variation; inferential statistics are undefined");
        if(armA<2 || selected.size()-armA<2) throw new IllegalArgumentException("At least two independent units per contrast arm are required");
        if(selected.size()<=terms.size()) throw new IllegalArgumentException("Insufficient residual degrees of freedom after adjustment");
        OLSStatisticalInferenceEngine engine=new OLSStatisticalInferenceEngine();
        String[] clusterIds=selected.stream().map(ExperimentalUnit::clusterId).toArray(String[]::new);
        Map<String,Object> diagnostics=new LinkedHashMap<>();diagnostics.put("dependencyVersions",Map.of("commonsMath","3.6.1","pdfBox","3.0.3"));diagnostics.put("estimator",options.estimator());diagnostics.put("missingPolicy",options.missingPolicy());diagnostics.put("eligibleUnits",eligibleCount);diagnostics.put("excludedMissingEndpoints",eligibleCount-selected.size());int observedCount=0;for(boolean flag:observed)if(flag)observedCount++;
        diagnostics.put("observedEndpoints",observedCount);diagnostics.put("missingEndpoints",observed.length-observedCount);
        RegressionResult regression;
        if(options.estimator()==Estimator.IPW || options.estimator()==Estimator.AIPW) {
            List<String> nuisanceTerms=new ArrayList<>(List.of("Intercept"));nuisanceTerms.addAll(terms.subList(2,terms.size()));diagnostics.put("nuisanceCovariateTerms",List.copyOf(nuisanceTerms));
            double[][] covariates=new double[x.length][x[0].length-1];int[] treatment=new int[x.length];
            for(int i=0;i<x.length;i++){covariates[i][0]=1;treatment[i]=(int)x[i][1];for(int j=2;j<x[i].length;j++)covariates[i][j-1]=x[i][j];}
            var estimate=new org.plantcausal.causal.estimators.WeightedCausalEstimator().estimate(covariates,treatment,y,observed,clusterIds,
                org.plantcausal.causal.estimators.WeightedCausalEstimator.Method.valueOf(options.estimator().name()),options.missingPolicy()==MissingPolicy.IPCW,options.bootstrapReplicates(),options.seed(),options.overlapThreshold());
            regression=new RegressionResult(new double[]{estimate.effect()},new double[]{estimate.standardError()},new double[]{estimate.effect()/estimate.standardError()},new double[]{estimate.p()},new double[]{estimate.low95()},new double[]{estimate.high95()},Double.NaN);
            diagnostics.put("nuisanceModels",estimate.nuisanceModels());diagnostics.put("bootstrapDraws",estimate.bootstrapDraws());diagnostics.put("propensity",estimate.propensity());diagnostics.put("observationProbability",estimate.observationProbability());diagnostics.put("weights",estimate.weights());diagnostics.put("effectiveSampleSize",estimate.effectiveSampleSize());
            diagnostics.put("minPropensity",estimate.propensity().stream().mapToDouble(Double::doubleValue).min().orElseThrow());diagnostics.put("maxPropensity",estimate.propensity().stream().mapToDouble(Double::doubleValue).max().orElseThrow());diagnostics.put("maxWeight",estimate.weights().stream().mapToDouble(Double::doubleValue).max().orElseThrow());
            diagnostics.put("weightingPopulation","Eligible analysis units; individual-unit ATE, not equal-weighted-cluster ATE");diagnostics.put("inference","Cluster-resampling percentile bootstrap CI; bootstrap-SE normal Wald p-value; all nuisance models refitted");
            warnings.add("ATE identification additionally requires treatment exchangeability and positivity. Fitted probabilities are checked, not clipped.");
            if(options.missingPolicy()==MissingPolicy.IPCW)warnings.add("Outcome observation weighting requires missing-at-random conditional on treatment and selected baseline covariates, plus observation positivity.");
            terms=new ArrayList<>(List.of(t.estimand().contrast()+" ATE"));
        } else if(options.estimator()==Estimator.CLUSTER_OLS) {
            regression=new org.plantcausal.causal.estimators.ClusterRobustOLS().fit(x,y,clusterIds);
            long g=Arrays.stream(clusterIds).distinct().count();diagnostics.put("independentClusters",g);diagnostics.put("inferenceDf",g-1);diagnostics.put("inference","CR1 cluster sandwich with t(G-1) intervals");
            if(g<30)warnings.add("Fewer than 30 independent clusters; CR1 finite-sample inference can be unreliable.");
        } else regression=engine.fit(x,y);
        boolean weighted=options.estimator()==Estimator.IPW||options.estimator()==Estimator.AIPW;
        double[] residuals=weighted?new double[0]:engine.residuals(x,y,regression.coefficients());
        List<Double> fitted=new ArrayList<>(),residualList=new ArrayList<>();
        for(int i=0;i<residuals.length;i++) {fitted.add(y[i]-residuals[i]);residualList.add(residuals[i]);}
        if(y.length-terms.size()<10) warnings.add("Small residual degrees of freedom; intervals are unstable and model assumptions need particular care.");
        if(options.estimator()==Estimator.OLS)warnings.add("Classical homoskedastic errors; exact Student-t inference assumes independent normally distributed errors with common variance.");
        String interpretation=d.protocol().dataKind().equals("synthetic")?"SYNTHETIC_DEMONSTRATION":
            d.protocol().document().path("emulation").path("design").asText().equals("randomized")?"RANDOMIZED_ASSIGNMENT_CONTRAST_REQUIRES_DESIGN_REVIEW":(weighted?"ATE_ESTIMATE_REQUIRES_IDENTIFICATION_REVIEW":"ADJUSTED_ASSOCIATION_REQUIRES_IDENTIFICATION_REVIEW");
        if(options.missingPolicy()==MissingPolicy.COMPLETE_CASE && !d.protocol().dataKind().equals("synthetic"))interpretation="SELECTED_COMPLETE_CASE_ASSOCIATION";
        if(!snapshot.fingerprint().equals(workspace.fingerprint(directory))) throw new IOException("Project changed during analysis; run again");
        if(options.missingPolicy()==MissingPolicy.COMPLETE_CASE)warnings.add("Explicit complete-case endpoint selection: "+(eligibleCount-selected.size())+" excluded units. The original protocol ATE is not identified without additional missingness/selection assumptions; interpret as a selected-sample association.");
        return new Result("0.4.0",d.protocol().trialId(),snapshot.fingerprint(),d.protocol().dataKind(),t.outcome().variableId(),t.outcome().unit(),t.estimand().contrast(),interpretation,"NOT_ASSESSED",options,
            selected.stream().map(ExperimentalUnit::unitId).toList(),List.copyOf(terms),regression,y.length,weighted?0:y.length-x[0].length,armA,y.length-armA,List.copyOf(new LinkedHashSet<>(warnings)),List.copyOf(fitted),List.copyOf(residualList),Collections.unmodifiableMap(diagnostics));
    }
    private List<String> categories(List<ExperimentalUnit> units,boolean genotype,boolean enabled) {
        if(!enabled) return List.of();
        SortedSet<String> set=new TreeSet<>();
        for(var u:units) {
            String value=genotype?u.genotype():u.blockId();
            if(value.equals("NA")) throw new IllegalArgumentException("Selected categorical covariate has missing values"); set.add(value);
        }
        return List.copyOf(set);
    }
    private double value(DataPackage d,String unit,String variable,BigDecimal time,boolean baseline,List<String> warnings) {
        var records=d.observations().stream().filter(o->o.unitId().equals(unit) && o.variableId().equals(variable)
            && (baseline?o.time().signum()<=0:o.time().compareTo(time)==0)).toList();
        if(records.size()!=1) throw new IllegalArgumentException("Exactly one endpoint/baseline record required; pre-aggregate repeated/technical measurements: "+unit+":"+variable);
        var row=records.get(0);
        if(baseline && row.time().signum()==0) {
            var definition=d.variables().stream().filter(v->v.variableId().equals(variable)).findFirst().orElseThrow();
            if(!definition.timing().equals("before_first_treatment")) throw new IllegalArgumentException("Time-zero covariate requires timing=before_first_treatment: "+variable);
            warnings.add("Time-zero baseline measurements rely on declared before_first_treatment timing; verify source ordering.");
        }
        if(row.value().equals("NA")) throw new IllegalArgumentException("Missing values require a prespecified missing-data estimator; no rows are silently dropped: "+unit+":"+variable);
        try {double result=Double.parseDouble(row.value());if(!Double.isFinite(result)) throw new NumberFormatException();return result;}
        catch(NumberFormatException e) {throw new IllegalArgumentException("Invalid numeric value: "+unit+":"+variable,e);}
    }
}
