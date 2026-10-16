package org.plantcausal.loader;

import org.plantcausal.benchmark.PlantCausalPackage;
import org.plantcausal.data.PlantCausalDataset;
import org.plantcausal.data.PlantCausalDatasetBuilder;
import org.plantcausal.parser.TsvParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultPackageLoader {

    private final TsvParser parser = new TsvParser();

    public PlantCausalPackage inspect(Path directory){
        PlantCausalPackage pkg = new PlantCausalPackage();
        pkg.setRoot(directory);
        if(Files.exists(directory.resolve("protocol.json"))) pkg.setProtocol(directory.resolve("protocol.json"));
        if(Files.exists(directory.resolve("phenotype.tsv"))) pkg.setPhenotype(directory.resolve("phenotype.tsv"));
        if(Files.exists(directory.resolve("genotype.tsv"))) pkg.setGenotype(directory.resolve("genotype.tsv"));
        if(Files.exists(directory.resolve("environment.tsv"))) pkg.setEnvironment(directory.resolve("environment.tsv"));
        if(Files.exists(directory.resolve("omics.tsv"))) pkg.setOmics(directory.resolve("omics.tsv"));
        return pkg;
    }

    public PlantCausalDataset load(Path directory) throws IOException {
        PlantCausalPackage pkg = inspect(directory);
        PlantCausalDatasetBuilder builder = new PlantCausalDatasetBuilder().addTable("package", pkg);

        if(pkg.getPhenotype()!=null) builder.addTable("phenotype", parser.parse(pkg.getPhenotype()));
        if(pkg.getGenotype()!=null) builder.addTable("genotype", parser.parse(pkg.getGenotype()));
        if(pkg.getEnvironment()!=null) builder.addTable("environment", parser.parse(pkg.getEnvironment()));
        if(pkg.getOmics()!=null) builder.addTable("omics", parser.parse(pkg.getOmics()));

        return builder.build(directory.getFileName().toString());
    }
}
