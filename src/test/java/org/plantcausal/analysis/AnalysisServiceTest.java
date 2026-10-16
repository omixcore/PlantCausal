package org.plantcausal.analysis;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.plantcausal.desktop.WorkspaceService;
import org.plantcausal.core.PackageReader;
import static org.junit.jupiter.api.Assertions.*;
class AnalysisServiceTest {
 @TempDir Path temporary;
 private Path project;
 private final AnalysisService service=new AnalysisService();
 private final AnalysisService.Options options=new AnalysisService.Options(List.of("baseline_x"),false,false);
 @BeforeEach void copy() throws Exception {
  project=temporary.resolve("project");
  new WorkspaceService().copyProject(Path.of("examples/linear_verification"),project);
 }
 @Test void recoversKnownCoefficientAndInference() throws Exception {
  var r=service.analyze(project,options);
  assertArrayEquals(new double[]{4,2,.5},r.regression().coefficients(),1e-10);
  assertEquals(21,r.residualDf());assertEquals(24,r.sampleSize());assertEquals(12,r.armA());assertEquals(12,r.armB());
  assertEquals(.1091089451179962,r.regression().standardErrors()[1],1e-10);
  assertEquals(1.77309552714898,r.regression().lower95()[1],1e-10);
  assertEquals(2.22690447285102,r.regression().upper95()[1],1e-10);
  assertEquals("NOT_ASSESSED",r.identificationStatus());assertEquals("SYNTHETIC_DEMONSTRATION",r.interpretation());
 }
 @Test void reversingContrastReversesCoefficient() throws Exception {
  var node=PackageReader.JSON.readTree(project.resolve("protocol.json").toFile());
  ((com.fasterxml.jackson.databind.node.ObjectNode)node.path("target_trial").path("estimand")).put("contrast","B-A");
  Files.writeString(project.resolve("protocol.json"),node.toString());
  var r=service.analyze(project,options);assertEquals(-2,r.regression().coefficients()[1],1e-10);
  assertEquals(-2.22690447285102,r.regression().lower95()[1],1e-10);
 }
 @Test void rejectsMissingEndpointInsteadOfDroppingUnit() throws Exception {
  edit("observations.tsv","90\tresponse\t2.5\tNA","90\tresponse\tNA\tunknown");
  assertThrows(IllegalArgumentException.class,()->service.analyze(project,options));
 }
 @Test void rejectsSharedClusters() throws Exception {
  edit("units.tsv","U02\tU02\tNA","U02\tU01\tNA");
  assertThrows(IllegalArgumentException.class,()->service.analyze(project,options));
 }
 @Test void rejectsMediatorAdjustment() throws Exception {
  edit("variables.tsv","baseline_confounder","mediator");
  assertThrows(IllegalArgumentException.class,()->service.analyze(project,options));
 }
 @Test void rejectsPostTreatmentCovariate() throws Exception {
  edit("observations.tsv","\t-1\tbaseline_x","\t1\tbaseline_x");
  assertThrows(Exception.class,()->service.analyze(project,options));
 }
 @Test void rejectsRepeatedEndpointMeasurements() throws Exception {
  var lines=Files.readAllLines(project.resolve("observations.tsv"));
  String extra=lines.stream().filter(s->s.contains("\tU01\t90\tresponse")).findFirst().orElseThrow().replace("\tNA\tNA\tSYNTHETIC","\tEXTRA\tREP\tSYNTHETIC");
  Files.writeString(project.resolve("observations.tsv"),Files.readString(project.resolve("observations.tsv"))+extra+"\n");
  assertThrows(IllegalArgumentException.class,()->service.analyze(project,options));
 }
 @Test void rejectsUnsupportedEffectScale() throws Exception {
  edit("protocol.json","mean_difference","mean_ratio");
  assertThrows(IllegalArgumentException.class,()->service.analyze(project,options));
 }
 @Test void exportContainsInputsAndNumericTables() throws Exception {
  var r=service.analyze(project,options);Path out=temporary.resolve("result");
  new AnalysisExporter().export(project,r,out);
  assertTrue(Files.size(out.resolve("PlantCausal_Report.pdf"))>1000);
  assertEquals(r.inputSha256(),new WorkspaceService().fingerprint(out.resolve("inputs")));
  var json=PackageReader.JSON.readTree(out.resolve("analysis.json").toFile());
  assertEquals(2,json.path("regression").path("coefficients").get(1).asDouble(),1e-10);
  assertEquals(25,Files.readAllLines(out.resolve("diagnostics.tsv")).size());
  assertEquals(4,Files.readAllLines(out.resolve("coefficients.tsv")).size());
 }
 @Test void staleAnalysisCannotBeExported() throws Exception {
  var r=service.analyze(project,options);edit("observations.tsv","2.5","2.6");
  Path out=temporary.resolve("stale");assertThrows(Exception.class,()->new AnalysisExporter().export(project,r,out));assertFalse(Files.exists(out));
 }
 @Test void outputCannotOverwriteProject() throws Exception {
  var r=service.analyze(project,options);assertThrows(Exception.class,()->new AnalysisExporter().export(project,r,project));
 }
 @Test void optionsAndResultAreDefensive() throws Exception {
  var list=new ArrayList<>(List.of("baseline_x"));var o=new AnalysisService.Options(list,false,false);list.clear();assertEquals(1,o.baselineCovariates().size());
  var r=service.analyze(project,options);r.regression().coefficients()[1]=999;assertEquals(2,r.regression().coefficients()[1],1e-10);
 }
 @Test void rejectsConstantOutcome() throws Exception {
  Path file=project.resolve("observations.tsv");
  var lines=Files.readAllLines(file);List<String> changed=new ArrayList<>();
  for(String line:lines) {
   if(line.contains("\tresponse\t")){String[] cells=line.split("\t",-1);cells[4]="5";line=String.join("\t",cells);}changed.add(line);
  }
  Files.write(file,changed);assertThrows(IllegalArgumentException.class,()->service.analyze(project,options));
 }
 @Test void rejectsOutputThroughSymlinkInsideProject() throws Exception {
  Path link=temporary.resolve("alias");
  try {Files.createSymbolicLink(link,project);} catch(UnsupportedOperationException|java.io.IOException e) {org.junit.jupiter.api.Assumptions.assumeTrue(false,"Symlink support unavailable");}
  var r=service.analyze(project,options);
  assertThrows(Exception.class,()->new AnalysisExporter().export(project,r,link.resolve("output")));
  assertFalse(Files.exists(project.resolve("output")));
 }
 private void edit(String name,String before,String after) throws Exception {
  Path path=project.resolve(name);String text=Files.readString(path);assertTrue(text.contains(before),"Fixture mutation missing");Files.writeString(path,text.replace(before,after));
 }
}
