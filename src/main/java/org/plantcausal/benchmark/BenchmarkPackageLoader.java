package org.plantcausal.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads PlantCausal benchmark packages from local directories.
 */
public class BenchmarkPackageLoader {

    public BenchmarkPackage load(Path packagePath) {
        if (!Files.exists(packagePath)) {
            throw new IllegalArgumentException("Benchmark package not found: " + packagePath);
        }
        return new BenchmarkPackage(packagePath.toString());
    }

    public record BenchmarkPackage(String location) {}
}
