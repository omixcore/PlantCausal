package org.plantcausal.service;

import org.plantcausal.schema.TargetTrial;
import org.plantcausal.schema.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class ValidationService {

    private final List<ValidatorRule> rules = new ArrayList<>();

    public ValidationService(){
        rules.add(new BasicSchemaRule());
        rules.add(new BiologicalLogicRule());
    }

    public ValidationResult validate(TargetTrial trial){
        ValidationResult result = new ValidationResult();
        for(ValidatorRule rule: rules){
            rule.validate(trial,result);
        }
        return result;
    }
}
