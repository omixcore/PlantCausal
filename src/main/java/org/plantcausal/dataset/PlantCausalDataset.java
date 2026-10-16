package org.plantcausal.dataset;

import java.util.List;
import java.util.Map;

/**
 * Standardized dataset representation for PlantCausal analysis.
 */
public record PlantCausalDataset(
        List<String> samples,
        Map<String, Map<String, String>> phenotype,
        Map<String, Map<String, String>> genotype,
        Map<String, Map<String, String>> environment,
        String treatment,
        String outcome
) {}
