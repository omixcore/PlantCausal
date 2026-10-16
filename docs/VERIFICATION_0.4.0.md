# Verification — PlantCausal 0.4.0

Validated on 2026-10-13 (Asia/Shanghai), Java 17, Maven 3.9.9.

91 automated tests passed, zero failures/errors/skips. Local Maven uses a forked Eclipse Java compiler because this runtime has Java without javac; GitHub Actions uses full Temurin JDK 17. The workaround is outside application source.

## Independent numerical references

- Existing NumPy/SciPy OLS fixtures, finite-df Student-t values and Cauchy special case remain verified.
- CR1 matches statsmodels: example treatment coefficient 2.5, SE 0.3118047822311617 (within floating-point tolerance).
- Baseline MAR example: IPW 1.9400523822882292; AIPW 1.994271317868655. Propensity/observation probabilities match independent statsmodels logistic fits.
- Longitudinal example: MSM_IPW 2.954201370544272 with 59 observed all-A and 61 all-B paths. The independent generator/reference is `tools/generate_longitudinal_verification.py`.

For the packaged JAR with default 300 bootstrap replicates/seed 20261013, the baseline AIPW example gives SE 0.1695755428, 95% percentile CI [1.6726872068,2.3128435500], ESS 154.9904. The sequential example gives SE 0.2687604476, CI [2.4304031961,3.4920553206], ESS 83.0511. These are demonstration outputs, not biological estimates.

## Behavior and integration

Tests cover:

- Original schema, identifier, time, allocation, role, missingness and public-workbook import checks.
- OLS rank/scale/df checks, robust variance, logistic class/rank/separation handling and explicit bootstrap/overlap limits.
- IPW/AIPW under MAR missing outcomes, complete-case count reporting, death-truncated endpoint rejection, stale input export checks and immutable result arrays.
- Sequential history timing, explicit grid requirement, wrong estimand and dropout-with-FAIL rejection. An independent multi-boundary dropout fixture verifies the shrinking risk sets (500,468,445), excludes histories after loss and matches statsmodels estimate 2.513470385394597.
- Exact input snapshot exports, per-unit weights, sequential probability tables, nuisance models and bootstrap draws; undefined R-squared serializes as JSON null.
- Actual Swing buttons load/run OLS, baseline causal and longitudinal demos. Changed parameters clear old results.

The executable JAR ran baseline AIPW/IPCW and MSM_IPW/IPCW workflows and generated actual JSON/TSV/PDF reports. Both PDFs were rendered using Poppler and visually inspected. Native Windows dialogs and platform-specific interactions were not executed locally; Windows launch scripts were reviewed. Headless Swing workflow tests are not a claim of native Windows end-to-end testing.

## Limits

This verifies supported numerical/workflow behavior, not universal causal identification, bootstrap coverage or validity for every study. Dynamic regimes, survival/competing risks, multi-arm treatments, MNAR methods, multiple imputation and cross-fitted machine-learning nuisances remain outside this release.
