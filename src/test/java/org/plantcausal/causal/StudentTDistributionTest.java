package org.plantcausal.causal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StudentTDistributionTest {
 @Test void allLegacyDistributionAdaptersUseSameNumerics() {
  var exact=new ExactStudentTDistribution();
  for(int df:new int[]{1,2,5,10,30,100}) for(double t:new double[]{0,.1,1,2,5,20}) {
   double reference=exact.twoSidedPValue(t,df);
   assertEquals(reference,new StudentTDistribution().twoSidedPValue(t,df),1e-12);
   assertEquals(reference,new ExactStudentTCalculator().twoSidedPValue(t,df),1e-12);
   assertEquals(reference,new StudentTCDFEngine().twoSidedPValue(t,df),1e-12);
   assertEquals(reference,new ExactStudentTDistributionEngine().twoSidedPValue(t,df),1e-12);
   assertEquals(reference,new OLSConfidenceInterval().twoSidedPValue(t,df),1e-12);
  }
 }
 @Test void cauchySpecialCaseIsAnalytic() {
  var d=new ExactStudentTDistribution();
  for(double t:new double[]{-10,-1,-.1,0,.1,1,10}) assertEquals(.5+Math.atan(t)/Math.PI,d.cdf(t,1),1e-12);
 }
 @Test void rejectsInvalidDistributionParameters() {
  assertThrows(IllegalArgumentException.class,()->new ExactStudentTDistribution().twoSidedPValue(1,0));
  assertThrows(IllegalArgumentException.class,()->new ExactStudentTDistribution().twoSidedPValue(Double.NaN,1));
 }
}
