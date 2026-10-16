package org.plantcausal.report;
import java.util.Map;
import java.io.*;
import org.apache.commons.csv.CSVFormat;
public class CsvCausalReportExporter {
 public String export(Map<String,Object> values) {
  StringWriter writer=new StringWriter();
  try(var csv=CSVFormat.DEFAULT.builder().setHeader("metric","value").build().print(writer)) {
   for(var entry:values.entrySet()) csv.printRecord(entry.getKey(),entry.getValue());
  } catch(IOException e){throw new UncheckedIOException(e);}
  return writer.toString();
 }
}
