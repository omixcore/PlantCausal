package org.plantcausal.core;
import java.nio.file.*;
public final class Main {
    public static void main(String[] args) throws Exception {
        if(args.length==2 && args[0].equals("fetch-benchmark")) {new BenchmarkFetcher().fetch(Path.of(args[1]));return;}
        if(args.length==3 && args[0].equals("import-pine")) {new BenchmarkImporter().importPine(Path.of(args[1]),Path.of(args[2]));return;}
        if(args.length==3 && args[0].equals("stage-grass")) {new BenchmarkImporter().stageGrass(Path.of(args[1]),Path.of(args[2]));return;}
        if(args.length!=2 || !args[0].equals("validate")) {
            System.err.println("Usage: validate <directory> | import-pine <xlsx> <output> | stage-grass <xls> <output>");System.exit(2);
        }
        var report=new ValidationService().validate(Path.of(args[1]));
        System.out.println(PackageReader.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        if(!report.schemaValid()) System.exit(1);
    }
}
