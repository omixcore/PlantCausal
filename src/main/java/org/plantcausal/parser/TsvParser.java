package org.plantcausal.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class TsvParser {

    public TsvTable parse(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        if (lines.isEmpty()) {
            return new TsvTable(List.of(), List.of());
        }
        List<String> headers = Arrays.asList(lines.get(0).split("\\t"));
        List<List<String>> rows = lines.subList(1, lines.size())
                .stream()
                .map(x -> Arrays.asList(x.split("\\t", -1)))
                .toList();
        return new TsvTable(headers, rows);
    }
}
