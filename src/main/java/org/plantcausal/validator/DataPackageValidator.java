package org.plantcausal.validator;
import org.plantcausal.schema.ValidationResult;
import java.nio.file.Path;
public class DataPackageValidator {
 public ValidationResult validate(Path directory) {
  var result=new ValidationResult();
  if(directory==null){result.addError("Missing package directory");return result;}
  var report=new org.plantcausal.core.ValidationService().validate(directory);
  report.issues().forEach(i->{if(i.severity().equals("ERROR"))result.addError(i.code()+": "+i.message());else result.addWarning(i.code()+": "+i.message());});return result;
 }
}
