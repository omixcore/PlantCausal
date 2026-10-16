package org.plantcausal.report;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.plantcausal.causal.*;

/** PDFBox 3 report with actual fitted estimates and explicit demo limitations. */
public class PdfBoxCausalReportGenerator {
 public void generate(Path output) throws IOException {
  write(output,List.of("PlantCausal Report","No fitted regression supplied. Run MaizeSaltStressDemoRunner for a complete report."));
 }
 public void generate(Path output,String trial,RegressionResult r,List<String> terms,int n,int df,CausalGraph graph) throws IOException {
  if(terms.size()!=r.coefficients().length) throw new IllegalArgumentException("Term count mismatch");
  List<String> lines=new ArrayList<>();
  lines.add("PlantCausal Analysis Report");
  lines.add("Maize salt stress - illustrative demo");
  lines.add("Trial: "+trial);
  lines.add("Data provenance: hand-entered demo records; not a validated public dataset.");
  lines.add("Model: biomass ~ salt + genotype (additive OLS, intercept included)");
  lines.add("Environment: shared growth environment; omitted as a constant covariate.");
  lines.add(String.format(Locale.ROOT,"Samples: %d    Residual degrees of freedom: %d    R-squared: %.6f",n,df,r.rSquared()));
  lines.add("Contrast: salt minus control; genotype reference is the first sorted category.");
  lines.add("Biomass units: unspecified in the supplied demo.");
  lines.add(""); lines.add("Regression estimates");
  for(int i=0;i<terms.size();i++) {
   lines.add(terms.get(i));
   lines.add(String.format(Locale.ROOT,"  Estimate: %.6f    SE: %.6f    t: %.6f    p: %.6g",
    r.coefficients()[i],r.standardErrors()[i],r.tStatistics()[i],r.pValues()[i]));
   lines.add(String.format(Locale.ROOT,"  95%% Student-t CI: [%.6f, %.6f]",r.lower95()[i],r.upper95()[i]));
  }
  lines.add(""); lines.add("Assumed DAG (specified, not learned from data)");
  for(var edge:graph.getEdges()) lines.add(edge.getSource().getName()+" -> "+edge.getTarget().getName());
  lines.add(""); lines.add("Interpretation and limits");
  lines.add("Classical homoskedastic OLS errors; finite-df Student-t p-values and intervals.");
  lines.add("The four-record example has only one residual degree of freedom.");
  lines.add("These coefficients demonstrate software operation; they do not establish causality.");
  lines.add("Causal interpretation requires justified assignment, exchangeability, positivity,");
  lines.add("consistency and an appropriate design/model. No biological claim is made here.");
  write(output,lines);
 }
 public void generateAnalysis(Path output,org.plantcausal.analysis.AnalysisService.Result result) throws IOException {
  List<String> lines=new ArrayList<>();
  lines.add("PlantCausal 0.4.0 - Analysis Report");
  lines.add("Trial: "+result.trialId()); lines.add("Interpretation: "+result.interpretation());
  lines.add("Identification status: "+result.identificationStatus());
  lines.add("Endpoint: "+result.outcome()+" ("+result.unit()+")");
  lines.add("Contrast: "+result.contrast()+"; first strategy minus second strategy");
  lines.add("Estimator: "+result.options().estimator());
  lines.add("Missing outcome policy: "+result.options().missingPolicy());
  lines.add("Inference: "+result.estimatorDiagnostics().getOrDefault("inference","Homoskedastic errors; finite-df Student-t"));
  lines.add("Input SHA-256: "+result.inputSha256());
  lines.add(String.format(Locale.ROOT,"n=%d   first baseline arm=%d   second baseline arm=%d",result.sampleSize(),result.armA(),result.armB()));
  lines.add(""); lines.add("Estimates and 95% confidence intervals");
  var r=result.regression();
  for(int i=0;i<result.terms().size();i++) {
   lines.add(result.terms().get(i));
   lines.add(String.format(Locale.ROOT,"  Estimate %.6g | SE %.6g | statistic %.6g | p %.6g",r.coefficients()[i],r.standardErrors()[i],r.tStatistics()[i],r.pValues()[i]));
   lines.add(String.format(Locale.ROOT,"  95%% CI [%.6g, %.6g]",r.lower95()[i],r.upper95()[i]));
  }
  lines.add("");lines.add("Estimator diagnostics");
  for(String key:List.of("effectiveSampleSize","minPropensity","maxPropensity","maxWeight","independentClusters","inferenceDf","eligibleUnits","excludedMissingEndpoints","observedEndpoints","missingEndpoints","sustainedArmA","sustainedArmB"))
   if(result.estimatorDiagnostics().containsKey(key))lines.add(key+": "+result.estimatorDiagnostics().get(key));
  if(Double.isFinite(r.rSquared()))lines.add("R-squared: "+r.rSquared());
  if(result.options().estimator()==org.plantcausal.analysis.AnalysisService.Estimator.OLS)lines.add("Residual df: "+result.residualDf());
  lines.add(""); lines.add("Design and interpretation limitations");
  for(String warning:result.warnings()) lines.add(warning);
  lines.add(""); lines.add("Reproducibility files: analysis.json, coefficients.tsv, diagnostics.tsv, inputs/");
  lines.add("Non-ASCII identifiers are escaped in this PDF; JSON/TSV retain original Unicode.");
  write(output,lines);
 }
 private String printable(String line) {
  StringBuilder result=new StringBuilder();
  for(char c:line.toCharArray()) {
   if(c>=32 && c<=126) result.append(c);
   else result.append(String.format(Locale.ROOT,"\\u%04x",(int)c));
  }
  return result.toString();
 }
 public void write(Path output,List<String> lines) throws IOException {
  Path absolute=output.toAbsolutePath(); Files.createDirectories(absolute.getParent());
  try(PDDocument doc=new PDDocument()) {
   PDFont font;
   try(var in=PdfBoxCausalReportGenerator.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
    if(in==null) throw new IOException("Bundled report font missing");
    font=PDType0Font.load(doc,in);
   }
   List<String> wrapped=new ArrayList<>();
   for(String line:lines) {
    String remaining=printable(line);
    while(font.getStringWidth(remaining)/1000*10>491) {
     int cut=remaining.length()-1;
     while(cut>1 && font.getStringWidth(remaining.substring(0,cut))/1000*10>491) cut--;
     int space=remaining.lastIndexOf(' ',cut); if(space>0) cut=space;
     wrapped.add(remaining.substring(0,cut)); remaining=remaining.substring(cut).stripLeading();
    }
    wrapped.add(remaining);
   }
   for(int start=0;start<wrapped.size();start+=43) {
    PDPage page=new PDPage(PDRectangle.A4); doc.addPage(page);
    try(PDPageContentStream stream=new PDPageContentStream(doc,page)) {
     stream.beginText(); stream.setFont(font,10); stream.setLeading(17); stream.newLineAtOffset(52,785);
     for(int i=start;i<Math.min(start+43,wrapped.size());i++) {stream.showText(wrapped.get(i)); stream.newLine();}
     stream.endText();
    }
   }
   doc.save(absolute.toFile());
  }
 }
}
