package org.plantcausal.benchmark;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.plantcausal.validator.PlantCausalValidator;
import org.plantcausal.dataset.TSVPlantCausalDatasetLoader;
import org.plantcausal.causal.*;
import org.plantcausal.core.PackageReader;
import org.plantcausal.report.PdfBoxCausalReportGenerator;

/** Executable illustrative demo; its four records are not a public-data benchmark. */
public class MaizeSaltStressDemoRunner {
 public record DemoResult(RegressionResult regression,CausalGraph graph,List<String> terms,int sampleSize,int df,Path report) {}
 public static void main(String[] args) throws Exception {
  if(args.length!=0 && args.length!=2) throw new IllegalArgumentException("Usage: MaizeSaltStressDemoRunner [package-directory output.pdf]");
  var result=new MaizeSaltStressDemoRunner().run(
   args.length==0?Path.of("benchmark/examples/maize_salt_stress"):Path.of(args[0]),
   args.length==0?Path.of("PlantCausal_Report.pdf"):Path.of(args[1]));
  System.out.printf(Locale.ROOT,"n=%d df=%d salt coefficient=%.6f p=%.6f report=%s%n",
   result.sampleSize(),result.df(),result.regression().coefficients()[1],result.regression().pValues()[1],result.report());
 }
 public void run() {
  try { run(Path.of("benchmark/examples/maize_salt_stress"),Path.of("PlantCausal_Report.pdf")); }
  catch(IOException e) { throw new IllegalStateException("Demo failed",e); }
 }
 public DemoResult run(Path directory,Path output) throws IOException {
  var pkg=new BenchmarkPackageLoader().load(directory);
  var validation=new PlantCausalValidator().validate(pkg);
  if(!validation.valid()) throw new IllegalArgumentException(validation.message());
  var data=new TSVPlantCausalDatasetLoader().load(directory);
  List<String> genotypes=new ArrayList<>();
  Set<String> environments=new HashSet<>();
  boolean treated=false,control=false;
  for(String id:data.samples()) {
   var row=data.phenotype().get(id); String genotype=data.genotype().get(id).get("genotype");
   if(genotype==null || genotype.isBlank() || !genotype.equals(row.get("genotype"))) throw new IllegalArgumentException("Genotype mismatch: "+id);
   if(!genotypes.contains(genotype)) genotypes.add(genotype);
   String env=data.environment().get(id).get("environment");
   if(env==null || env.isBlank()) throw new IllegalArgumentException("Missing environment: "+id);
   environments.add(env);
  }
  // This demo controls environment by design; variable environments need a specified model.
  if(environments.size()!=1) throw new IllegalArgumentException("Demo requires a single shared environment");
  Collections.sort(genotypes);
  List<String> terms=new ArrayList<>(List.of("Intercept","Salt (vs control)"));
  for(int j=1;j<genotypes.size();j++) terms.add(genotypes.get(j)+" (vs "+genotypes.get(0)+")");
  double[][] x=new double[data.samples().size()][terms.size()]; double[] y=new double[x.length];
  for(int i=0;i<x.length;i++) {
   String id=data.samples().get(i); var row=data.phenotype().get(id);
   String treatment=row.get("treatment");
   if(!"salt".equals(treatment) && !"control".equals(treatment)) throw new IllegalArgumentException("Invalid treatment: "+id);
   boolean salt="salt".equals(treatment); treated|=salt; control|=!salt;
   double dose=number(data.environment().get(id).get("salt_concentration"),id);
   if(dose<0 || salt!=(dose>0)) throw new IllegalArgumentException("Treatment/dose mismatch: "+id);
   x[i][0]=1; x[i][1]=salt?1:0;
   for(int j=1;j<genotypes.size();j++) x[i][j+1]=genotypes.get(j).equals(row.get("genotype"))?1:0;
   y[i]=number(row.get("biomass"),id);
  }
  if(!treated || !control) throw new IllegalArgumentException("Both control and salt groups are required");
  RegressionResult regression=new OLSStatisticalInferenceEngine().fit(x,y);
  CausalGraph graph=new DAGBuilder().addRelation("Salt","Biomass").addRelation("Genotype","Biomass").build();
  String trial=PackageReader.JSON.readTree(directory.resolve("protocol.json").toFile()).path("trial_id").asText();
  new PdfBoxCausalReportGenerator().generate(output,trial,regression,terms,x.length,x.length-terms.size(),graph);
  return new DemoResult(regression,graph,List.copyOf(terms),x.length,x.length-terms.size(),output.toAbsolutePath());
 }
 private double number(String value,String id) {
  try {double n=Double.parseDouble(value); if(!Double.isFinite(n)) throw new NumberFormatException(); return n;}
  catch(Exception e) {throw new IllegalArgumentException("Missing or non-finite numeric value: "+id,e);}
 }
}
