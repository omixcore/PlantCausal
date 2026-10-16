package org.plantcausal.validator;

import org.plantcausal.schema.ValidationResult;

import java.nio.file.Path;

public interface PackageRule {

    void validate(Path packageDirectory, ValidationResult result);
}
