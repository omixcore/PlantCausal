package org.plantcausal.validator;
import java.nio.file.*;
import org.plantcausal.benchmark.BenchmarkPackageLoader.BenchmarkPackage;
import org.plantcausal.core.PackageReader;
/** Structural checks for the legacy demo format, distinct from schema v1 validation. */
public class PlantCausalValidator {
 public ValidationResult validate(BenchmarkPackage pkg) {
  if(pkg==null || pkg.location()==null || pkg.location().isBlank()) return new ValidationResult(false,"Missing benchmark package");
  try {
   Path dir=Path.of(pkg.location());
   for(String file:new String[]{"protocol.json","phenotype.tsv","genotype.tsv","environment.tsv"})
    if(!Files.isRegularFile(dir.resolve(file))) return new ValidationResult(false,"Missing file: "+file);
   var protocol=PackageReader.JSON.readTree(dir.resolve("protocol.json").toFile());
   for(String field:new String[]{"trial_id","species","intervention","design","outcome"})
    if(!protocol.path(field).isTextual() || protocol.path(field).asText().isBlank()) return new ValidationResult(false,"Missing protocol field: "+field);
   return new ValidationResult(true,"Legacy demo structure validated");
  } catch(Exception e) { return new ValidationResult(false,"Invalid demo package: "+e.getMessage()); }
 }
 public record ValidationResult(boolean valid,String message) {}
}
