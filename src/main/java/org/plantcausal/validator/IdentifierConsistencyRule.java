package org.plantcausal.validator;
import org.plantcausal.schema.ValidationResult;
import java.nio.file.Path;
/** Compatibility rule: standard validation is centralized to avoid partial passes. */
public class IdentifierConsistencyRule implements PackageRule {
 public void validate(Path directory,ValidationResult result) {
  var checked=new DataPackageValidator().validate(directory);
  checked.getErrors().forEach(result::addError);checked.getWarnings().forEach(result::addWarning);
 }
}
