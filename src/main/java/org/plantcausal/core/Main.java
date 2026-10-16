package org.plantcausal.core;
import java.nio.file.*;
public final class Main {
    public static void main(String[] args) throws Exception {
        if(args.length==1 && (args[0].equals("--help")||args[0].equals("-h"))) {
            System.out.println("PlantCausal 0.4.0\nUsage: gui | validate <project> | analyze <project> <empty-output-folder> [--covariates=id1,id2] [--genotype] [--block] [--estimator=OLS|CLUSTER_OLS|IPW|AIPW|MSM_IPW] [--missing=FAIL|COMPLETE_CASE|IPCW] [--time-varying=id1,id2] [--bootstrap=300] [--seed=20261013] [--overlap=0.01] | demo-maize [package-directory output.pdf] | import-pine <xlsx> <output> | stage-grass <xls> <output> | fetch-benchmark <output>");return;
        }
        if(args.length==0 || (args.length==1&&args[0].equals("gui"))) {org.plantcausal.desktop.DesktopApp.launch();return;}
        if(args[0].equals("analyze")) {
            if(args.length<3) throw new IllegalArgumentException("Usage: analyze <project> <output-directory> [--covariates=id1,id2] [--genotype] [--block]");
            java.util.List<String> covariates=new java.util.ArrayList<>(); boolean genotype=false,block=false;
            var estimator=org.plantcausal.analysis.AnalysisService.Estimator.OLS;
            var missing=org.plantcausal.analysis.AnalysisService.MissingPolicy.FAIL;
            java.util.List<String> timeVarying=new java.util.ArrayList<>();int bootstrap=300;long seed=20261013L;double overlap=.01;
            for(int i=3;i<args.length;i++) {
                if(args[i].startsWith("--covariates=")) {
                    if(!covariates.isEmpty()) throw new IllegalArgumentException("Repeated covariate option");
                    String value=args[i].substring(13); if(!value.isBlank()) covariates.addAll(java.util.Arrays.asList(value.split(",",-1)));
                } else if(args[i].equals("--genotype")) genotype=true;
                else if(args[i].equals("--block")) block=true;
                else if(args[i].startsWith("--time-varying="))timeVarying.addAll(java.util.Arrays.asList(args[i].substring(15).split(",",-1)));
                else if(args[i].startsWith("--estimator="))estimator=org.plantcausal.analysis.AnalysisService.Estimator.valueOf(args[i].substring(12).toUpperCase(java.util.Locale.ROOT));
                else if(args[i].startsWith("--missing="))missing=org.plantcausal.analysis.AnalysisService.MissingPolicy.valueOf(args[i].substring(10).toUpperCase(java.util.Locale.ROOT));
                else if(args[i].startsWith("--bootstrap="))bootstrap=Integer.parseInt(args[i].substring(12));
                else if(args[i].startsWith("--seed="))seed=Long.parseLong(args[i].substring(7));
                else if(args[i].startsWith("--overlap="))overlap=Double.parseDouble(args[i].substring(10));
                else throw new IllegalArgumentException("Unknown analysis option: "+args[i]);
            }
            Path input=Path.of(args[1]),output=Path.of(args[2]);
            var result=new org.plantcausal.analysis.AnalysisService().analyze(input,new org.plantcausal.analysis.AnalysisService.Options(covariates,genotype,block,estimator,missing,bootstrap,seed,overlap,timeVarying));
            new org.plantcausal.analysis.AnalysisExporter().export(input,result,output);
            System.out.println(PackageReader.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result));return;
        }
        if(args[0].equals("demo-maize")) {
            org.plantcausal.benchmark.MaizeSaltStressDemoRunner.main(java.util.Arrays.copyOfRange(args,1,args.length)); return;
        }
        if(args.length==2 && args[0].equals("fetch-benchmark")) {new BenchmarkFetcher().fetch(Path.of(args[1]));return;}
        if(args.length==3 && args[0].equals("import-pine")) {new BenchmarkImporter().importPine(Path.of(args[1]),Path.of(args[2]));return;}
        if(args.length==3 && args[0].equals("stage-grass")) {new BenchmarkImporter().stageGrass(Path.of(args[1]),Path.of(args[2]));return;}
        if(args.length!=2 || !args[0].equals("validate")) {
            System.err.println("Usage: analyze <project> <output> [options] | demo-maize [package-directory output.pdf] | validate <directory> | import-pine <xlsx> <output> | stage-grass <xls> <output>");System.exit(2);
        }
        var report=new ValidationService().validate(Path.of(args[1]));
        System.out.println(PackageReader.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        if(!report.schemaValid()) System.exit(1);
    }
}
