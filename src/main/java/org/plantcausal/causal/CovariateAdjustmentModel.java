package org.plantcausal.causal;
import org.plantcausal.data.PlantCausalDataset;
import org.plantcausal.parser.TsvTable;
import java.util.*;
/** Legacy binary-treatment table adapter; identification is not inferred from a fit. */
public class CovariateAdjustmentModel implements CausalModel {
 private final List<String> covariates;
 public CovariateAdjustmentModel(){this(List.of());}
 public CovariateAdjustmentModel(List<String> covariates){this.covariates=List.copyOf(covariates);}
 @Override public CausalResult estimate(PlantCausalDataset dataset) {
  if(dataset==null || !(dataset.getTables().get("phenotype") instanceof TsvTable)) throw new IllegalArgumentException("Phenotype table required");
  var table=(TsvTable)dataset.getTables().get("phenotype");
  int treatment=table.getHeaders().indexOf("treatment"),outcome=table.getHeaders().indexOf("outcome");
  if(treatment<0||outcome<0) throw new IllegalArgumentException("Numeric treatment (0/1) and outcome columns required");
  int[] columns=covariates.stream().mapToInt(table.getHeaders()::indexOf).toArray();
  for(int column:columns) if(column<0||column==treatment||column==outcome) throw new IllegalArgumentException("Invalid covariate column");
  double[][] x=new double[table.getRows().size()][2+columns.length];double[] y=new double[x.length];
  int treated=0;
  for(int i=0;i<x.length;i++) {
   var row=table.getRows().get(i);if(row.size()!=table.getHeaders().size()) throw new IllegalArgumentException("Invalid row length");
   double t=Double.parseDouble(row.get(treatment)); if(t!=0&&t!=1) throw new IllegalArgumentException("Treatment must be 0/1");
   treated+=(int)t;x[i][0]=1;x[i][1]=t;y[i]=Double.parseDouble(row.get(outcome));
   for(int j=0;j<columns.length;j++) x[i][j+2]=Double.parseDouble(row.get(columns[j]));
  }
  if(treated<2||x.length-treated<2) throw new IllegalArgumentException("Two units per arm required");
  var regression=new OLSStatisticalInferenceEngine().fit(x,y);var result=new CausalResult();
  result.setAverageTreatmentEffect(regression.coefficients()[1]);result.setStandardError(regression.standardErrors()[1]);
  result.setConfidenceLow(regression.lower95()[1]);result.setConfidenceHigh(regression.upper95()[1]);result.setPValue(regression.pValues()[1]);
  result.setSampleSize(y.length);result.setDiagnostics("Additive OLS association; legacy table lacks design metadata. Use AnalysisService for standard data and design checks.");return result;
 }
}
