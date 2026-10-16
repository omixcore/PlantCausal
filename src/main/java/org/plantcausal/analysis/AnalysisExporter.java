package org.plantcausal.analysis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.apache.commons.csv.CSVFormat;
import org.plantcausal.core.PackageReader;
import org.plantcausal.desktop.WorkspaceService;
import org.plantcausal.report.PdfBoxCausalReportGenerator;

/** Exports reproducible results, inputs, parameters and residual diagnostics. */
public final class AnalysisExporter {
    public void export(Path input,AnalysisService.Result result,Path output) throws Exception {
        WorkspaceService workspace=new WorkspaceService();
        if(!result.inputSha256().equals(workspace.fingerprint(input))) throw new IOException("Input changed since analysis; run analysis again");
        Path source=input.toRealPath(),dest=output.toAbsolutePath().normalize();
        Path ancestor=dest;
        while(!Files.exists(ancestor)) ancestor=ancestor.getParent();
        dest=ancestor.toRealPath().resolve(ancestor.relativize(dest)).normalize();
        if(Files.exists(dest)) dest=dest.toRealPath();
        if(dest.equals(source)||dest.startsWith(source)) throw new IOException("Choose an output directory outside the input project");
        Files.createDirectories(dest);
        try(var files=Files.list(dest)) {if(files.findAny().isPresent()) throw new IOException("Output directory must be empty");}
        List<Path> written=new ArrayList<>();
        try {
            Path json=dest.resolve("analysis.json");written.add(json);
            Files.writeString(json,PackageReader.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result)+"\n",StandardCharsets.UTF_8);
            Path table=dest.resolve("coefficients.tsv");written.add(table);
            try(var writer=Files.newBufferedWriter(table);var csv=CSVFormat.TDF.builder().setHeader("term","estimate","standard_error","statistic","p","lower95","upper95").build().print(writer)) {
                var r=result.regression();
                for(int i=0;i<result.terms().size();i++) csv.printRecord(result.terms().get(i),r.coefficients()[i],r.standardErrors()[i],r.tStatistics()[i],r.pValues()[i],r.lower95()[i],r.upper95()[i]);
            }
            Path diagnostics=dest.resolve("diagnostics.tsv");written.add(diagnostics);
            try(var writer=Files.newBufferedWriter(diagnostics);var csv=CSVFormat.TDF.builder().setHeader("unit_id","fitted","residual").build().print(writer)) {
                for(int i=0;i<result.fitted().size();i++) csv.printRecord(result.unitIds().get(i),result.fitted().get(i),result.residuals().get(i));
            }
            if(result.estimatorDiagnostics().containsKey("weights")) {
                Path weightTable=dest.resolve("weights.tsv");written.add(weightTable);
                var weights=(java.util.List<?>)result.estimatorDiagnostics().get("weights");
                var propensity=(java.util.List<?>)result.estimatorDiagnostics().get("propensity");
                var observation=(java.util.List<?>)result.estimatorDiagnostics().get("observationProbability");
                try(var writer=Files.newBufferedWriter(weightTable);var csv=CSVFormat.TDF.builder().setHeader("unit_id","weight","propensity","observation_probability").build().print(writer)) {
                    for(int i=0;i<result.sampleSize();i++)csv.printRecord(result.unitIds().get(i),weights.get(i),propensity==null?"NA":propensity.get(i),observation==null?"NA":observation.get(i));
                }
            }
            if(result.estimatorDiagnostics().containsKey("propensityHistory")) {
                Path historyTable=dest.resolve("sequential_weights.tsv");written.add(historyTable);
                var probabilities=(java.util.List<?>)result.estimatorDiagnostics().get("propensityHistory");
                var observation=(java.util.List<?>)result.estimatorDiagnostics().get("observationHistory");
                var grid=(java.util.List<?>)result.estimatorDiagnostics().get("timeGrid");
                try(var writer=Files.newBufferedWriter(historyTable);var csv=CSVFormat.TDF.builder().setHeader("unit_id","decision_time","arm_a_probability","next_observation_probability").build().print(writer)) {
                    for(int i=0;i<result.sampleSize();i++) {
                        var e=(java.util.List<?>)probabilities.get(i);var r=(java.util.List<?>)observation.get(i);
                        for(int k=0;k<e.size();k++)csv.printRecord(result.unitIds().get(i),grid.get(k),e.get(k)==null?"NA":e.get(k),r.get(k)==null?"NA":r.get(k));
                    }
                }
            }
            Path inputs=dest.resolve("inputs");Files.createDirectory(inputs);written.add(inputs);
            for(String name:WorkspaceService.FILES) Files.copy(source.resolve(name),inputs.resolve(name));
            if(!result.inputSha256().equals(workspace.fingerprint(inputs)) || !result.inputSha256().equals(workspace.fingerprint(source))) throw new IOException("Input changed while exporting");
            Path pdf=dest.resolve("PlantCausal_Report.pdf");written.add(pdf);
            new PdfBoxCausalReportGenerator().generateAnalysis(pdf,result);
        } catch(Exception e) {
            for(Path path:written) {
                if(Files.isDirectory(path)) try(var files=Files.list(path)){for(Path f:files.toList()) Files.deleteIfExists(f);}
                Files.deleteIfExists(path);
            }
            throw e;
        }
    }
}
