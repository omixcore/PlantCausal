package org.plantcausal.causal;

import org.plantcausal.data.PlantCausalDataset;

public interface CausalModel {

    CausalResult estimate(PlantCausalDataset dataset);
}
