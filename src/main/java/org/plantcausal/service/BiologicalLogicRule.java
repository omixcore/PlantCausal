package org.plantcausal.service;

import org.plantcausal.schema.TargetTrial;
import org.plantcausal.schema.ValidationResult;

public class BiologicalLogicRule implements ValidatorRule {

    public void validate(TargetTrial trial, ValidationResult result){
        if(trial==null) return;

        if(trial.getPopulation()!=null && trial.getPopulation().getSampleSize()<=0){
            result.addError("Population sample size must be greater than zero");
        }

        if(trial.getDataSource()==null){
            result.addWarning("No provenance information available");
        }
    }
}
