package org.plantcausal.integration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.plantcausal.benchmark.MaizeSaltStressDemoRunner;
import static org.junit.jupiter.api.Assertions.*;
class MaizeSaltStressPipelineTest {
 @TempDir Path temporary;
 @Test void runMaizeSaltStressPipeline() {
  assertDoesNotThrow(()->new MaizeSaltStressDemoRunner().run(Path.of("benchmark/examples/maize_salt_stress"),temporary.resolve("pipeline.pdf")));
 }
}
