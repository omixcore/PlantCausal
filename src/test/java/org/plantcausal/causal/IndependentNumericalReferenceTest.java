package org.plantcausal.causal;
import org.junit.jupiter.api.Test;
import org.plantcausal.core.PackageReader;
import static org.junit.jupiter.api.Assertions.*;
class IndependentNumericalReferenceTest {
 @Test void matchesIndependentNumpyScipyFixturesAcrossScales() throws Exception {
  try(var in=getClass().getResourceAsStream("/numpy-scipy-reference.json")) {
   for(var test:PackageReader.JSON.readTree(in)) {
    var x=PackageReader.JSON.treeToValue(test.get("x"),double[][].class);
    var y=PackageReader.JSON.treeToValue(test.get("y"),double[].class);
    var result=new OLSStatisticalInferenceEngine().fit(x,y);
    check(test.get("beta"),result.coefficients());check(test.get("se"),result.standardErrors());check(test.get("p"),result.pValues());
    check(test.get("low"),result.lower95());check(test.get("high"),result.upper95());
   }
  }
 }
 private void check(com.fasterxml.jackson.databind.JsonNode expected,double[] actual) {
  for(int i=0;i<actual.length;i++) assertEquals(expected.get(i).asDouble(),actual[i],Math.max(1e-10,Math.abs(expected.get(i).asDouble())*1e-9));
 }
 @Test void rejectsNearCollinearColumns() {
  double[][] x={{1,0,1e-12},{1,1,1+1e-12},{1,2,2-1e-12},{1,3,3+2e-12}};
  assertThrows(IllegalArgumentException.class,()->new OLSStatisticalInferenceEngine().fit(x,new double[]{1,2,4,5}));
 }
 @Test void constantOutcomeHasUndefinedRSquared() {
  var result=new OLSStatisticalInferenceEngine().fit(new double[][]{{1,0},{1,1},{1,2},{1,3}},new double[]{5,5,5,5});
  assertTrue(Double.isNaN(result.rSquared()));assertEquals(0,result.coefficients()[1],1e-10);
 }
}
