package org.plantcausal.report;
import java.io.IOException;
import java.nio.file.*;
import org.plantcausal.causal.CausalResult;
import org.plantcausal.core.PackageReader;
public class CausalReportExporter {
 public void exportJson(CausalResult result,Path output) throws IOException {
  Files.writeString(output,PackageReader.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result)+"\n");
 }
}
