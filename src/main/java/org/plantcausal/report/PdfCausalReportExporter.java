package org.plantcausal.report;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
public class PdfCausalReportExporter {
 public void export(String text,Path output) throws IOException {
  new PdfBoxCausalReportGenerator().write(output,Arrays.asList(text.split("\\R",-1)));
 }
}
