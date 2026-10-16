package org.plantcausal.analysis;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.plantcausal.desktop.WorkspaceService;
import org.plantcausal.core.PackageReader;
import static org.junit.jupiter.api.Assertions.*;
class LongitudinalAnalysisTest {
 @TempDir Path temporary;
 private AnalysisService.Options options(){return new AnalysisService.Options(List.of("baseline_z"),false,false,AnalysisService.Estimator.MSM_IPW,AnalysisService.MissingPolicy.IPCW,200,123,.001,List.of("history_l"));}
 @Test void matchesIndependentStatsmodelsSequentialWeightReferenceAndExports() throws Exception {
  var result=new AnalysisService().analyze(Path.of("examples/longitudinal_verification"),options());
  try(var in=getClass().getResourceAsStream("/longitudinal-statsmodels-reference.json")) {
   var reference=PackageReader.JSON.readTree(in);assertEquals(reference.path("effect").asDouble(),result.regression().coefficients()[0],1e-8);
   assertEquals(reference.path("regimeA").asInt(),result.estimatorDiagnostics().get("sustainedArmA"));assertEquals(reference.path("regimeB").asInt(),result.estimatorDiagnostics().get("sustainedArmB"));
  }
  assertTrue(result.regression().lower95()[0]<result.regression().upper95()[0]);
  new AnalysisExporter().export(Path.of("examples/longitudinal_verification"),result,temporary.resolve("export"));
  assertTrue(PackageReader.JSON.readTree(temporary.resolve("export/analysis.json").toFile()).path("regression").path("rSquared").isNull());
 }
 @Test void missingGridAndPostDecisionMeasurementsAreRejected() throws Exception {
  Path project=temporary.resolve("project");new WorkspaceService().copyProject(Path.of("examples/longitudinal_verification"),project);
  Path obs=project.resolve("observations.tsv");String text=Files.readString(obs);Files.writeString(obs,text.replace("\t29\thistory_l","\t30\thistory_l"));
  assertThrows(IllegalArgumentException.class,()->new AnalysisService().analyze(project,options()));
 }
 @Test void multiBoundaryCensoringUsesOnlyAtRiskHistory() throws Exception {
  Path input=Path.of("src/test/resources/longitudinal_dropout");
  var result=new AnalysisService().analyze(input,options());
  var reference=PackageReader.JSON.readTree(input.resolve("reference.json").toFile());
  assertEquals(reference.path("effect").asDouble(),result.regression().coefficients()[0],1e-8);
  var models=(java.util.List<?>)result.estimatorDiagnostics().get("nuisanceModels");
  for(int k=0;k<models.size();k++)assertEquals(reference.path("riskSetSizes").get(k).asInt(),((java.util.Map<?,?>)models.get(k)).get("riskSetSize"));
  var probabilities=(java.util.List<?>)result.estimatorDiagnostics().get("propensityHistory");
  assertTrue(probabilities.stream().anyMatch(row->((java.util.List<?>)row).contains(null)),"Histories after loss must remain unmodeled");
 }
 @Test void missingGridIsRejected() throws Exception {
  Path project=temporary.resolve("no-grid");new WorkspaceService().copyProject(Path.of("examples/longitudinal_verification"),project);
  var node=PackageReader.JSON.readTree(project.resolve("protocol.json").toFile());((com.fasterxml.jackson.databind.node.ObjectNode)node.path("extensions")).remove("longitudinal");
  Files.writeString(project.resolve("protocol.json"),node.toString());assertThrows(IllegalArgumentException.class,()->new AnalysisService().analyze(project,options()));
 }
 @Test void failPolicyDoesNotIgnoreDropout() {
  var o=new AnalysisService.Options(List.of("baseline_z"),false,false,AnalysisService.Estimator.MSM_IPW,AnalysisService.MissingPolicy.FAIL,200,123,.001,List.of("history_l"));
  assertThrows(IllegalArgumentException.class,()->new AnalysisService().analyze(Path.of("examples/longitudinal_verification"),o));
 }
 @Test void wrongEstimandIsRejected() {
  assertThrows(IllegalArgumentException.class,()->new AnalysisService().analyze(Path.of("examples/linear_verification"),new AnalysisService.Options(List.of(),false,false,AnalysisService.Estimator.MSM_IPW,AnalysisService.MissingPolicy.FAIL,200,1,.01,List.of())));
 }
}
