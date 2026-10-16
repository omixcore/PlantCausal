package org.plantcausal.causal;

import org.plantcausal.math.ExactStudentTDistribution;

/** Exact Student-t inference bridge. */
public class StudentTInferenceEngine {
    public double twoSidedPValue(double t, int degreesOfFreedom) {
        return ExactStudentTDistribution.twoSidedP(t, degreesOfFreedom);
    }
}
