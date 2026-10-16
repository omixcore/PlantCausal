package org.plantcausal.benchmark;

import java.nio.file.Path;

/**
 * Standard PlantCausal data package definition.
 */
public class PlantCausalPackage {

    private Path root;
    private Path protocol;
    private Path phenotype;
    private Path genotype;
    private Path environment;
    private Path omics;

    public Path getRoot(){ return root; }
    public void setRoot(Path root){ this.root = root; }

    public Path getProtocol(){ return protocol; }
    public void setProtocol(Path protocol){ this.protocol = protocol; }

    public Path getPhenotype(){ return phenotype; }
    public void setPhenotype(Path phenotype){ this.phenotype = phenotype; }

    public Path getGenotype(){ return genotype; }
    public void setGenotype(Path genotype){ this.genotype = genotype; }

    public Path getEnvironment(){ return environment; }
    public void setEnvironment(Path environment){ this.environment = environment; }

    public Path getOmics(){ return omics; }
    public void setOmics(Path omics){ this.omics = omics; }
}
