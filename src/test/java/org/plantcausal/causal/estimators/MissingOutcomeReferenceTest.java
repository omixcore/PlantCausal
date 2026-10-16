package org.plantcausal.causal.estimators;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.plantcausal.core.PackageReader;
import static org.junit.jupiter.api.Assertions.*;
class MissingOutcomeReferenceTest {
 @Test void matchesIndependentStatsmodelsPointEstimatesWithMarMissingness() throws Exception {
  try(var in=getClass().getResourceAsStream("/weighted-statsmodels-reference.json")) {
   var node=PackageReader.JSON.readTree(in);var x=PackageReader.JSON.treeToValue(node.get("x"),double[][].class);
   var t=PackageReader.JSON.treeToValue(node.get("t"),int[].class);var y=PackageReader.JSON.treeToValue(node.get("y"),double[].class);var r=PackageReader.JSON.treeToValue(node.get("observed"),boolean[].class);
   String[] g=new String[t.length];for(int i=0;i<t.length;i++){g[i]="G"+i;if(!r[i])y[i]=Double.NaN;}
   for(var method:WeightedCausalEstimator.Method.values()) {
    var result=new WeightedCausalEstimator().estimate(x,t,y,r,g,method,true,200,123,.001);
    assertEquals(node.path(method.name().toLowerCase(Locale.ROOT)).asDouble(),result.effect(),1e-8);
    for(int i=0;i<t.length;i++){assertEquals(node.get("propensity").get(i).asDouble(),result.propensity().get(i),1e-8);assertEquals(node.get("observation").get(i).asDouble(),result.observationProbability().get(i),1e-8);}
   }
  }
 }
}
