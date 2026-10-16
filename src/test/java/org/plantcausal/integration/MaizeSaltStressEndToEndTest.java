package org.plantcausal.integration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.plantcausal.benchmark.MaizeSaltStressDemoRunner;
import static org.junit.jupiter.api.Assertions.*;
class MaizeSaltStressEndToEndTest {
 @TempDir Path temporary;
 @Test void runMaizeSaltStressPipeline() throws Exception {
  Path output=temporary.resolve("nested/PlantCausal_Report.pdf");
  var result=new MaizeSaltStressDemoRunner().run(Path.of("benchmark/examples/maize_salt_stress"),output);
  assertEquals(4,result.sampleSize()); assertEquals(1,result.df());
  assertArrayEquals(new double[]{10,-2,2.3},result.regression().coefficients(),1e-10);
  assertEquals(.4,result.regression().standardErrors()[1],1e-10);
  // Independent NumPy least-squares / SciPy Student-t reference values.
  assertEquals(.1256659163780024,result.regression().pValues()[1],1e-9);
  assertEquals(-7.08248189457284,result.regression().lower95()[1],1e-8);
  assertEquals(3.08248189457284,result.regression().upper95()[1],1e-8);
  assertEquals(3,result.graph().getNodes().size()); assertEquals(2,result.graph().getEdges().size());
  assertTrue(Files.size(output)>1000);
  try(var document=Loader.loadPDF(output.toFile())) {
   assertEquals(1,document.getNumberOfPages());
   String text=new PDFTextStripper().getText(document);
   assertTrue(text.contains("Estimate: -2.000000")); assertTrue(text.contains("p: 0.125666"));
   assertTrue(text.contains("not a validated public dataset")); assertTrue(text.contains("Residual degrees of freedom: 1"));
  }
 }
 @Test void rejectsMismatchedSamplesWithoutCreatingReport() throws Exception {
  Path source=Path.of("benchmark/examples/maize_salt_stress"), copy=temporary.resolve("input");
  Files.createDirectory(copy);
  for(String name:new String[]{"protocol.json","phenotype.tsv","genotype.tsv","environment.tsv"}) Files.copy(source.resolve(name),copy.resolve(name));
  Files.writeString(copy.resolve("genotype.tsv"),Files.readString(copy.resolve("genotype.tsv")).replace("S4","other"));
  Path output=temporary.resolve("rejected.pdf");
  assertThrows(IllegalArgumentException.class,()->new MaizeSaltStressDemoRunner().run(copy,output));
  assertFalse(Files.exists(output));
 }
 @Test void rejectsMissingProtocol() {
  Path output=temporary.resolve("missing.pdf");
  assertThrows(IllegalArgumentException.class,()->new MaizeSaltStressDemoRunner().run(temporary,output));
  assertFalse(Files.exists(output));
 }
}
