package org.plantcausal.validator;
import org.plantcausal.schema.ValidationResult;
import org.plantcausal.desktop.WorkspaceService;
import java.nio.file.*;
public class JsonSchemaValidator {
 public ValidationResult validate(Path jsonFile) {
  var result=new ValidationResult();
  try {new WorkspaceService().checkProtocol(Files.readString(jsonFile));}
  catch(Exception e){result.addError("Protocol schema validation failed: "+e.getMessage());}
  return result;
 }
}
