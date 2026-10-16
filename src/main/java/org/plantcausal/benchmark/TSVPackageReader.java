package org.plantcausal.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Lightweight TSV reader used by benchmark packages.
 */
public class TSVPackageReader {

    public List<String> readLines(Path file) throws IOException {
        return Files.readAllLines(file);
    }
}
