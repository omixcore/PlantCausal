package org.plantcausal.service;

import org.plantcausal.schema.TargetTrial;
import org.plantcausal.schema.ValidationResult;

public class BasicSchemaRule implements ValidatorRule {

    public void validate(TargetTrial trial, ValidationResult result){
        if(trial == null){
            result.addError("Target trial is null");
            return;
        }
        if(trial.getTrialId()==null || trial.getTrialId().isBlank()){
            result.addError("Missing required field: trialId");
        }
        if(trial.getSpecies()==null || trial.getSpecies().isBlank()){
            result.addError("Missing required field: species");
        }
        if(trial.getPopulation()==null){
            result.addError("Missing required field: population");
        }
        if(trial.getIntervention()==null){
            result.addError("Missing required field: intervention");
        }
        if(trial.getOutcomes()==null || trial.getOutcomes().isEmpty()){
            result.addError("At least one outcome is required");
        }
    }
}
