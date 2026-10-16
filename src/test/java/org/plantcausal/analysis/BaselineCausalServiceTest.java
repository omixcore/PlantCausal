package org.plantcausal.analysis;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.plantcausal.desktop.WorkspaceService;
import org.plantcausal.core.PackageReader;
import static org.junit.jupiter.api.Assertions.*;
class BaselineCausalServiceTest {
 @TempDir Path temporary;
 private AnalysisService.Options options(AnalysisService.Estimator method,AnalysisService.MissingPolicy missing) {return new AnalysisService.Options(List.of("baseline_x1","baseline_x2"),false,false,method,missing,200,123,.001);}
 @Test void standardPackageAipwMatchesGoldenReferenceAndExportsWeights() throws Exception {
  Path input=Path.of("examples/baseline_causal_verification");
  var result=new AnalysisService().analyze(input,options(AnalysisService.Estimator.AIPW,AnalysisService.MissingPolicy.IPCW));
  try(var in=getClass().getResourceAsStream("/weighted-statsmodels-reference.json")){assertEquals(PackageReader.JSON.readTree(in).path("aipw").asDouble(),result.regression().coefficients()[0],1e-8);}
  new AnalysisExporter().export(input,result,temporary.resolve("export"));
  assertEquals(241,Files.readAllLines(temporary.resolve("export/weights.tsv")).size());
  assertTrue(result.estimatorDiagnostics().containsKey("effectiveSampleSize"));assertEquals(240,result.sampleSize());
 }
 @Test void failPolicyRejectsMissingValuesAndCompleteCaseIsExplicit() throws Exception {
  assertThrows(IllegalArgumentException.class,()->new AnalysisService().analyze(Path.of("examples/baseline_causal_verification"),options(AnalysisService.Estimator.OLS,AnalysisService.MissingPolicy.FAIL)));
  var result=new AnalysisService().analyze(Path.of("examples/baseline_causal_verification"),options(AnalysisService.Estimator.OLS,AnalysisService.MissingPolicy.COMPLETE_CASE));
  assertTrue(result.sampleSize()<240);assertTrue((int)result.estimatorDiagnostics().get("excludedMissingEndpoints")>0);
 }
 @Test void invalidBootstrapOrOverlapIsRejected() {
  assertThrows(IllegalArgumentException.class,()->new AnalysisService.Options(List.of(),false,false,AnalysisService.Estimator.AIPW,AnalysisService.MissingPolicy.FAIL,200,1,Double.NaN));
  assertThrows(IllegalArgumentException.class,()->new AnalysisService.Options(List.of(),false,false,AnalysisService.Estimator.AIPW,AnalysisService.MissingPolicy.FAIL,10,1,.01));
 }
 @Test void deathTruncatedOutcomeCannotBeWeightedAsMar() throws Exception {
  Path input=temporary.resolve("input");new WorkspaceService().copyProject(Path.of("examples/baseline_causal_verification"),input);
  String row=Files.readAllLines(input.resolve("observations.tsv")).stream().filter(line->line.contains("\tresponse\tNA\t")).findFirst().orElseThrow();String id=row.split("\t")[1];
  Files.writeString(input.resolve("events.tsv"),Files.readString(input.resolve("events.tsv"))+"SYNTHETIC_BASELINE_CAUSAL_240\t"+id+"\t60\tdeath\tgenerated test\tSYNTHETIC\n");
  assertThrows(IllegalArgumentException.class,()->new AnalysisService().analyze(input,options(AnalysisService.Estimator.AIPW,AnalysisService.MissingPolicy.IPCW)));
 }
}
