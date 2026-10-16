package org.plantcausal.integration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import org.plantcausal.core.Main;
import static org.junit.jupiter.api.Assertions.*;
class MaizeSaltStressRealPipelineTest {
 @TempDir Path temporary;
 @Test void helpIsAvailable(){assertDoesNotThrow(()->Main.main(new String[]{"--help"}));}
 @Test void cliExportsDemoReport() throws Exception {
  Path report=temporary.resolve("cli.pdf");
  Main.main(new String[]{"demo-maize","benchmark/examples/maize_salt_stress",report.toString()});
  assertTrue(Files.size(report)>1000);
 }
}
