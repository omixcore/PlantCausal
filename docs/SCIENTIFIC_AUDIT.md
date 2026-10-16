# Scientific audit — PlantCausal 0.4.0

## Identification is separate from execution

A target trial specifies eligibility, strategies, assignment, time zero, follow-up, outcomes and contrast. Emulating these definitions cannot manufacture exchangeability, positivity, correct measurement, consistency or no interference. All results retain `identificationStatus=NOT_ASSESSED`; synthetic data remain demonstrations. An estimator runs under assumptions, rather than automatically proving its assumptions.

The primary contrast is first strategy minus second, on the continuous endpoint's mean-difference scale. Baseline methods target baseline assignment/exposure; they are never relabeled as sustained/per-protocol effects. MSM_IPW explicitly targets only the two static regimes A at all decisions and B at all decisions.

## OLS and cluster inference

Scaled QR fits coefficients; SVD checks ill-conditioning. Independent OLS uses SSE/(n-p), coefficient covariance from the QR pseudoinverse, and finite-df Student-t inference. Exact Student-t justification requires independent normal homoskedastic errors.

CR1 covariance is `(X'X)^-1 * sum_g(S_g S_g') * (X'X)^-1` with `S_g=sum_i_in_g X_i residual_i`. The correction is `G/(G-1)*(n-1)/(n-p)`; intervals use t(G-1). CR1 is an approximate cluster-robust procedure, not an exact small-cluster guarantee. At least three documented independent clusters are required; fewer than 30 produces a warning. Shared allocation units may not be split across inference clusters. Variance correction does not remove cluster-level confounding or interference bias.

## Baseline IPW/AIPW

Let T=1 indicate the first strategy, X the selected pre-treatment covariates, e(X)=P(T=1|X), R indicate observed endpoint and pi(T,X)=P(R=1|T,X). Logistic models are unpenalized MLE, scaled with rank/convergence/separation checks. No ridge is silently introduced and no fitted probabilities are silently clipped.

IPW uses arm-normalized (Hájek) weighted means with weights `R/[P(T|X)*pi(T,X)]`. Without missing outcomes pi=1. The target is an individual-unit population mean contrast, not an equal-weighted-cluster estimand.

AIPW averages

`m1(X)-m0(X) + T*R*(Y-m1(X))/(e(X)*pi(T,X)) - (1-T)*R*(Y-m0(X))/((1-e(X))*pi(T,X))`.

Separate arm-specific linear regressions fit m1/m0 on observed endpoints. Under the identification/MAR assumptions, consistency follows if the outcome models are correct or the treatment and observation models are both correct. With complete endpoints, the usual treatment-or-outcome double robustness applies. It does not protect against unmeasured confounding, MNAR missingness or both nuisance components being wrong.

## Missingness and selection

FAIL rejects missing outcomes/covariates. COMPLETE_CASE explicitly removes missing endpoints and records counts; for nonsynthetic studies it is labeled a selected-sample association, because selection after treatment does not automatically identify the protocol population effect. IPCW models outcome observation/retention under conditional MAR/independent censoring and observation positivity.

Covariate/history missingness is rejected; no default zero or mean fill is used. Death-truncated missing outcomes require a defined alternative estimand, not ordinary MAR weighting. Multiple imputation and survival/competing-risk methods are not implemented.

## Longitudinal sustained-regime estimator

An explicit discrete grid starts at zero and ends at the primary endpoint. Each at-risk interval has one verified actual strategy. First strategy agrees with the baseline classification. Numeric time-varying confounders are observed strictly before the decision, after the preceding boundary; initial time-zero values require a before-first-treatment declaration. Unknown actual adherence/history, gaps, duplicate measurements and post-decision data are rejected.

At step k, the treatment model includes the intercept, prespecified baseline predictors, all observed confounder histories through k and all prior treatments. The retention model additionally includes current treatment. Histories after loss are not used. Recorded losses must lie on grid boundaries. Retention probabilities equal one at fully observed steps; a step with no observed retention has no support and is rejected.

For observed subjects who follow a sustained regime, the weight is the product of inverse probabilities of their actual treatment and next retention at each decision. Regime means are normalized separately. Nonadherers and censored subjects receive zero contribution to those regime means. No numerator stabilization or truncation is hidden. Sequential exchangeability, positivity, consistency, correct models and an appropriate interference structure remain required.

This is an IPW marginal comparison of static sustained regimes. It is not dynamic-policy learning, a survival MSM, clone-censor-weight analysis of arbitrary rules, or automatic treatment of death as dropout.

## Uncertainty and diagnostics

IPW/AIPW/MSM resample entire documented inference clusters, including all members/histories. Every bootstrap replicate refits all relevant propensity, outcome and observation models. Failures cause an explicit error; failed draws are not dropped or substituted. Defaults are 300 replicates, seed 20261013, overlap threshold 0.01; at least 200 replicates and ten clusters are required.

Intervals are empirical percentile intervals. Standard errors are bootstrap sample SD. Reported p-values use a normal Wald approximation with that SE; they are not exact Student-t tests or bootstrap tail-count p-values. Adequate cluster counts, more replicates and an analysis-specific sensitivity plan are needed for final studies. Cross-fitted machine-learning nuisance models are not implemented.

Diagnostics preserve probabilities, weights, ESS, per-step risk sets/model coefficients, bootstrap draws, selected options, dependency versions and exact hashed input snapshots. No causal-readiness flag changes merely because a numerical test succeeds.

## Independent implementation checks

- CR1 coefficients and SE match statsmodels' independent cluster-robust implementation.
- MAR baseline fixture: statsmodels IPW=1.9400523822882292, AIPW=1.994271317868655. Java matches these point estimates and fitted probabilities.
- Sequential fixture: independent statsmodels treatment/retention fits give sustained-regime difference 2.954201370544272, with 59 observed all-A and 61 observed all-B subjects.
- Synthetic data are generated verification fixtures. Agreement with a reference implementation verifies calculation, not scientific identification or universal coverage.

## Primary references

- Hernán MA, Robins JM. *Causal Inference: What If*, especially chapters on inverse probability weighting and target-trial emulation. https://www.hsph.harvard.edu/miguel-hernan/wp-content/uploads/sites/1268/2024/04/hernanrobins_WhatIf_26apr24.pdf
- Funk MJ et al. Doubly Robust Estimation of Causal Effects. https://pmc.ncbi.nlm.nih.gov/articles/PMC3070495/
- Augmented Inverse Probability Weighting and the Double Robustness Property. https://pmc.ncbi.nlm.nih.gov/articles/PMC8793316/
- Hernán MA et al. Causal Inference from Longitudinal Studies with Baseline Randomization. https://pmc.ncbi.nlm.nih.gov/articles/PMC2835458/
- Apache Commons Math QR API. https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/linear/QRDecomposition.html
