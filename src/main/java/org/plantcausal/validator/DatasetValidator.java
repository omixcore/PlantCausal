package org.plantcausal.validator;

import org.plantcausal.dataset.PlantCausalDataset;

/**
 * Validates PlantCausalDataset consistency.
 */
public class DatasetValidator {

    public ValidationResult validate(PlantCausalDataset dataset) {
        if (dataset == null) {
            return new ValidationResult(false, "Dataset is null");
        }
        if (dataset.samples() == null || dataset.samples().isEmpty()) {
            return new ValidationResult(false, "No samples found");
        }
        if (dataset.outcome() == null || dataset.outcome().isBlank()) {
            return new ValidationResult(false, "Missing outcome variable");
        }
        if (dataset.treatment() == null || dataset.treatment().isBlank()) {
            return new ValidationResult(false, "Missing treatment variable");
        }
        return new ValidationResult(true, "Dataset validation passed");
    }

    public record ValidationResult(boolean valid, String message) {}
}
