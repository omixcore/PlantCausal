package org.plantcausal.report;
import java.io.*;
import java.util.*;
public class PdfBoxReportExporter {
 public File export(CausalReport report,File output) throws IOException {
  if(report==null||output==null) throw new IllegalArgumentException("Report and output are required");
  List<String> lines=new ArrayList<>(List.of("PlantCausal report","Benchmark: "+report.getBenchmarkId(),"Model: "+report.getModel()));
  report.getStatistics().forEach((key,value)->lines.add(key+": "+value));
  new PdfBoxCausalReportGenerator().write(output.toPath(),lines);return output;
 }
}
