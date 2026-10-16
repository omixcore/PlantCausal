# Verification — PlantCausal 0.3.0

Validated on 2026-10-13 (Asia/Shanghai), Linux, Java 17, Maven 3.9.9.

## Build and tests

`mvn clean verify` / `mvn verify`: executable shaded JAR built successfully. This environment's Java runtime lacks `javac`, so local Maven used a forked Eclipse Java 17 compiler; CI uses a full Temurin JDK 17 with the standard Maven compiler. Maven network access used the environment's proxy. These runtime workarounds are not included in application code.

74 tests passed, 0 failures, 0 errors, 0 skipped in the final test run. Coverage includes:

- 31 schema/data-logic validation tests and 3 public workbook importer tests.
- Eight workspace tests plus a real Swing button workflow: load the estimator demo, run analysis, verify coefficients and clear displayed results when parameters change.
- Standard analysis/export tests for known coefficients, reversed contrast, missing endpoints, shared clusters, mediator/post-treatment adjustment, repeated measurements, unsupported effect scales, input snapshots, stale exports, immutable results, constant endpoints and symlinked output safety.
- OLS rank, finite-value and residual-df guards; independent NumPy/SciPy reference matrices including predictors with very different scales.
- Student-t adapters, analytic Cauchy special case, finite-df critical values and invalid arguments.
- Maize pipeline and CLI execution tests, asserting actual PDF content and independent numeric reference values.

## Executable smoke checks

Successfully ran the packaged JAR to:

1. Validate the schema-v1 synthetic example.
2. Import the public pine workbook and validate the resulting standard package.
3. Stage the grassland workbook.
4. Run the legacy four-record maize demo and generate a loadable PDF.
5. Analyze the 24-unit linear fixture with `baseline_x` adjustment and export PDF, JSON, coefficient table, per-unit diagnostics and copied inputs.

NumPy/SciPy independently gives treatment coefficient 2, SE 0.10910894511799618, p 2.1278450765254012e-14 and 95% CI [1.7730955271489826, 2.2269044728510172] for the verification fixture.

## Visual inspection

Actual PDF output rendered through Poppler and was inspected for margins, text wrapping and legibility. An embedded DejaVu font corrected the earlier Standard-14 fallback rendering. Swing's maintained Analysis page was rendered and inspected; parameter controls and action buttons use separate rows, and result precision/column sizing support readable numeric inspection.

Desktop interaction was validated on Swing's event thread in a headless environment. Native Windows dialogs, installer integration and platform-specific filesystem behavior were not tested locally. The Windows launcher was reviewed, but no claim of a native Windows end-to-end test is made.

## Scope conclusion

The schema-v1 independent-unit, fixed-endpoint additive OLS workflow is executable and verified. This does not validate scientific identification for any supplied dataset. Advanced longitudinal, missing-data, survival and clustered estimators remain outside the supported scope listed in README and the scientific audit.
