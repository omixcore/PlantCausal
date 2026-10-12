package org.plantcausal.core;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

/** Records retain source IDs and avoid converting missing data to zero. */
public final class Models {
    private Models() {}
    public record Strategy(String id, String description) {}
    public record Outcome(String variableId, BigDecimal time, String unit, String definition) {}
    public record Estimand(String population, String contrast, String effect, String strategyEffect, String intercurrentEvents) {}
    public record TargetTrial(String population, String eligibility, String assignmentUnit, String analysisUnit,
            String assignment, String timeZero, String timeUnit, BigDecimal followupEnd,
            List<Strategy> strategies, Outcome outcome, Estimand estimand, String analysisPlan) {}
    public record Protocol(String schemaVersion, String trialId, String dataKind, TargetTrial targetTrial, JsonNode document) {}
    public record ExperimentalUnit(String trialId, String unitId, String parentUnitId, String unitType,
            String assignmentUnitId, String clusterId, String blockId, String species, String genotype,
            String timeZero, boolean eligible, String strategyId, String sourceId) {}
    public record VariableDefinition(String variableId, String role, String dataType, String unit,
            String definition, String timing, String missingCodes) {}
    public record Observation(String trialId, String unitId, BigDecimal time, String variableId, String value,
            String missingReason, String sampleId, String technicalReplicateId, String sourceId, String sourceLocation) {}
    public record TreatmentInterval(String trialId, String assignmentUnitId, BigDecimal start, BigDecimal stop,
            String strategyId, String exposureName, String dose, String doseUnit, String adherence, String sourceId) {}
    public record Event(String trialId, String unitId, BigDecimal time, String eventType, String reason, String sourceId) {}
    public record DataPackage(Protocol protocol, List<ExperimentalUnit> units, List<VariableDefinition> variables,
            List<Observation> observations, List<TreatmentInterval> treatments, List<Event> events) {}
    public record Issue(String severity, String code, String location, String message) {}
    public record ValidationReport(String schemaVersion, boolean schemaValid, String designReviewed,
            String analysisReady, List<Issue> issues, int unitCount, int observationCount) {}
}
