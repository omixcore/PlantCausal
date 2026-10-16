package org.plantcausal.validator;

import org.plantcausal.schema.ValidationResult;

import java.nio.file.Files;
import java.nio.file.Path;

public class RequiredFileRule implements PackageRule {

    @Override
    public void validate(Path packageDirectory, ValidationResult result) {
        String[] required = {"protocol.json"};

        for (String file : required) {
            if (!Files.exists(packageDirectory.resolve(file))) {
                result.addError("Missing required package file: " + file);
            }
        }
    }
}
