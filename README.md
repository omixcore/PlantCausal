# PlantCausal

Java 17 desktop software for plant target-trial protocols, data validation and reproducible causal effect estimation under explicit identification assumptions.

## Run

```bash
mvn clean verify
java -jar target/plantcausal-core-0.4.0.jar
```

Windows: double-click `start-PlantCausal.bat` with JDK 17+ and Maven installed. The packaged desktop ZIP or GitHub Actions executable JAR runs with Java 17+ without Maven.

## Desktop

Open/load a project → review and save protocol → validate → **Analysis** → select estimator and missing-outcome policy → inspect estimates/weights → export to an empty folder outside the project.

- **Load estimator demo**: 24 independent synthetic units, OLS with `baseline_x`.
- **Load causal demo**: 240 synthetic units, baseline IPW/AIPW and MAR missing outcomes; defaults to AIPW + IPCW with `baseline_x1,baseline_x2`.
- **Load longitudinal demo**: 500 synthetic units on three decisions, actual strategy switches and endpoint dropout; defaults to MSM_IPW + IPCW with `baseline_z` and history `history_l`.

These are numerical verification fixtures, not published biological observations. Public pine/grassland examples remain available for source-backed import and format validation; their true causal effects are unknown.

## Estimators

| Method | Supported target/design | Inference |
|---|---|---|
| `OLS` | Additive continuous endpoint contrast, independent units | Classical homoskedastic errors, Student-t with n-p df |
| `CLUSTER_OLS` | Same additive model, documented correlated clusters/nested allocation | CR1 sandwich, t(G-1), finite-sample factor |
| `IPW` | Baseline binary-strategy individual-unit ATE; logistic propensity | Hájek arm means; cluster bootstrap refits propensity/observation models |
| `AIPW` | Baseline binary-strategy individual-unit ATE; logistic propensity + separate arm outcome regressions | Augmented estimator; cluster bootstrap refits every nuisance model |
| `MSM_IPW` | Two static sustained regimes, A at every decision versus B at every decision, continuous endpoint | Sequential treatment/observation weights, Hájek regime means; cluster bootstrap |

Adjustment uses prespecified numeric baseline variables and optional genotype/block indicators. Longitudinal models use prespecified numeric baseline and time-varying histories; genotype/block indicators must be explicitly encoded as baseline variables for that method. Logistic MLE is unpenalized, with rank, convergence, separation and overlap checks.

Bootstrap defaults: 300 replicates, seed 20261013, overlap threshold 0.01. At least 200 replicates and 10 independent resampling clusters are required. Failed draws are not discarded or silently replaced. Bootstrap intervals are percentile intervals; p-values use a normal Wald approximation with the bootstrap standard error. They are not exact Student-t tests. More replicates and adequate cluster counts are advisable for final studies.

## Missing outcomes

| Policy | Behavior and interpretation |
|---|---|
| `FAIL` | Reject missing endpoints/covariates |
| `COMPLETE_CASE` | Explicitly exclude missing endpoints and report counts; selected-sample association unless extra selection assumptions justify a population effect |
| `IPCW` | IPW/AIPW: model endpoint observation given treatment and baseline. MSM: model next retention given pre-decision history and current treatment. Requires MAR/conditional independent censoring and observation positivity |

Baseline/history covariate missingness is rejected. Death-truncated outcomes are not automatically treated as ordinary MAR missingness; define a suitable estimand. Multiple imputation is not implemented.

## CLI

```bash
java -jar target/plantcausal-core-0.4.0.jar --help
java -jar target/plantcausal-core-0.4.0.jar validate examples/linear_verification
java -jar target/plantcausal-core-0.4.0.jar analyze examples/linear_verification output/ols --covariates=baseline_x
java -jar target/plantcausal-core-0.4.0.jar analyze examples/baseline_causal_verification output/aipw --estimator=AIPW --missing=IPCW --covariates=baseline_x1,baseline_x2
java -jar target/plantcausal-core-0.4.0.jar analyze examples/longitudinal_verification output/msm --estimator=MSM_IPW --missing=IPCW --covariates=baseline_z --time-varying=history_l
```

Additional options: `--genotype`, `--block`, `--bootstrap=1000`, `--seed=123`, `--overlap=0.01`.

Public import commands:

```bash
java -jar target/plantcausal-core-0.4.0.jar import-pine benchmark/raw/10100580/Mycorrhiza_exposure_drought.xlsx output/pine
java -jar target/plantcausal-core-0.4.0.jar stage-grass benchmark/raw/4306840/181004_Biomass_2014.xls output/grass
```

## Longitudinal input contract

Use schema-v1 tables and define `extensions.longitudinal.time_grid`, for example `[0,30,60,90]`. Grid starts at zero and ends at the primary endpoint. At each at-risk step there must be exactly one verified actual strategy interval `[grid[k],grid[k+1]]`. First actual strategy must agree with baseline strategy. Unknown adherence is rejected.

For each selected history variable, provide one measurement strictly before the current decision and after the previous grid boundary; the initial measurement precedes time zero (or is explicitly declared before-first-treatment at zero). The model includes all these measured histories and all previous treatments. Post-decision values are rejected. Loss events must lie on grid boundaries. Exposure history stops after loss; retained subjects require a measured endpoint. There is no automatic survival analysis, dynamic-policy learning or censoring of deaths.

## Export and reproducibility

Outputs include PDF, `analysis.json`, `coefficients.tsv`, individual diagnostics/weights, optional `sequential_weights.tsv`, and the exact six input files. JSON preserves software version, options, input SHA-256, IDs, target contrast, fitted nuisance coefficients, probabilities, weights, bootstrap draws, ESS and scientific warnings. Inputs are rechecked during analysis and export; changed parameters clear displayed estimates.

The contrast is always first strategy minus second. OLS estimates and IPW/AIPW estimates retain their actual model/inference labels. The historical JSON property `tStatistics` contains the displayed statistic; for bootstrap methods it is a normal Wald statistic, not a Student-t statistic. Undefined/not-applicable R-squared is `null`.

## Scientific limits

Format validation is not causal identification. Estimates require a defensible target trial, exchangeability (sequential for MSM), positivity, consistency, appropriate nuisance/model specification, and a justified interference/cluster structure. Cluster-robust variance corrects inference for correlation; it does not remove cluster-level confounding or interference bias. Doubly robust does not mean both nuisance models can be wrong.

The supported software workflow now includes baseline weighting, augmented estimation, cluster-robust inference, MAR observation weighting and static sustained-regime analysis. Dynamic regimes, survival/competing risks, multiple imputation, arbitrary multi-arm treatment, cross-fitted machine-learning nuisance models and automatic ontology/unit reconciliation remain outside scope. The maintained launcher is `org.plantcausal.core.Main` / `desktop.DesktopApp`; older standalone UI prototypes are compatibility scaffolding.

- [Data standard](DATA_STANDARD.md)
- [Scientific audit](docs/SCIENTIFIC_AUDIT.md)
- [0.4.0 verification](docs/VERIFICATION_0.4.0.md)
- [Public data provenance](benchmark/README.md)
