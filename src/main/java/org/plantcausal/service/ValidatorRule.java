package org.plantcausal.service;

import org.plantcausal.schema.TargetTrial;
import org.plantcausal.schema.ValidationResult;

public interface ValidatorRule {
    void validate(TargetTrial trial, ValidationResult result);
}
