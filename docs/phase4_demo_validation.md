# PlantCausal Phase 4 Demo Validation

## Exact Student-t validation

Validation targets:

- Gamma/log-Gamma numerical stability
- Regularized incomplete beta convergence
- Student-t CDF consistency
- Two-sided p-value calculation

Reference checks:

| statistic | df | expected check |
|---|---:|---|
| t=0 | 1,10,100 | p-value close to 1 |
| large | 10 | monotonic tail decrease |

## PDF report workflow

Causal report pipeline:

```
CausalResult
  -> CausalReport
  -> PDF generator
  -> PlantCausal_Report.pdf
```

Report sections:

- benchmark metadata
- target trial definition
- DAG
- effect estimate
- confidence interval
- statistical diagnostics

## Maize salt stress demo workflow

```
Load Demo
  -> Validate
  -> Generate DAG
  -> Run OLS
  -> Calculate CI/p-value
  -> Export report
```
