package org.plantcausal.causal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OLSStatisticalInferenceEngineTest {
 private final OLSStatisticalInferenceEngine engine=new OLSStatisticalInferenceEngine();
 @Test void rejectsSingularDesign() {
  assertThrows(IllegalArgumentException.class,()->engine.fit(new double[][]{{1,1},{1,1},{1,1}},new double[]{1,2,3}));
 }
 @Test void rejectsNoResidualDegreesOfFreedom() {
  assertThrows(IllegalArgumentException.class,()->engine.fit(new double[][]{{1,0},{1,1}},new double[]{1,2}));
 }
 @Test void rejectsNonFiniteResponse() {
  assertThrows(IllegalArgumentException.class,()->engine.fit(new double[][]{{1},{1}},new double[]{1,Double.NaN}));
 }
 @Test void exactFitUsesDegenerateInference() {
  var r=engine.fit(new double[][]{{1,0},{1,1},{1,2}},new double[]{1,3,5});
  assertArrayEquals(new double[]{1,2},r.coefficients(),1e-10);
  assertEquals(0,engine.tStatistic(0,0));
  assertEquals(Double.POSITIVE_INFINITY,engine.tStatistic(2,0));
 }
 @Test void finiteDfCriticalValuesMatchReference() {
  var distribution=new ExactStudentTDistribution();
  assertEquals(12.7062047364321,distribution.criticalValue95(1),1e-8);
  assertEquals(2.22813885196494,distribution.criticalValue95(10),1e-8);
  assertEquals(.5,distribution.cdf(0,10),1e-12);
  assertEquals(0,distribution.twoSidedPValue(Double.POSITIVE_INFINITY,10),1e-12);
 }
}
