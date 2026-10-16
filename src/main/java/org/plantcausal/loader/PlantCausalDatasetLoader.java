package org.plantcausal.loader;

import org.plantcausal.data.PlantCausalDataset;
import java.nio.file.Path;

public interface PlantCausalDatasetLoader {

    PlantCausalDataset load(Path packageDirectory);
}
