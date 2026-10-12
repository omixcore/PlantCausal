package org.plantcausal.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.plantcausal.core.Models.*;

/** Stateless service: a format pass never automatically grants causal readiness. */
public final class ValidationService {
    private final JsonSchema schema;
    public ValidationService() {
        try(var input=ValidationService.class.getResourceAsStream("/schema/protocol.schema.json")) {
            schema=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(PackageReader.JSON.readTree(input));
        } catch(Exception e) {throw new IllegalStateException("Cannot load bundled schema",e);}
    }
    public ValidationReport validate(Path directory) {
        List<Issue> issues=new ArrayList<>();
        PackageReader reader=new PackageReader();
        try {
            JsonNode x=reader.readProtocol(directory);
            schema.validate(x).stream().sorted(Comparator.comparing(Object::toString)).forEach(v->issues.add(new Issue("ERROR","PROTOCOL_SCHEMA","protocol.json",v.toString())));
            if(!issues.isEmpty()) return report(issues,0,0);
            return validate(reader.read(directory,x),issues);
        } catch(Exception e) {
            issues.add(new Issue("ERROR","PACKAGE_PARSE",directory.toString(),e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));
            return report(issues,0,0);
        }
    }
    private static ValidationReport report(List<Issue> issues,int units,int obs) {
        return new ValidationReport("1.0.0",issues.stream().noneMatch(i->i.severity().equals("ERROR")),"NOT_ASSESSED","NOT_ASSESSED",List.copyOf(issues),units,obs);
    }
    private ValidationReport validate(DataPackage d,List<Issue> out) {
        Protocol p=d.protocol(); TargetTrial t=p.targetTrial();
        class Checks {
            void test(boolean condition,String code,String at,String message) {if(!condition) out.add(new Issue("ERROR",code,at,message));}
            void warn(boolean condition,String code,String at,String message) {if(condition) out.add(new Issue("WARNING",code,at,message));}
            void trial(String id,String at) {test(p.trialId().equals(id),"TRIAL_ID",at,"trial_id differs from protocol");}
        }
        Checks c=new Checks();
        Map<String,ExperimentalUnit> units=new LinkedHashMap<>();Map<String,VariableDefinition> variables=new HashMap<>();Set<String> strategies=new HashSet<>();
        for(Strategy a:t.strategies()) c.test(strategies.add(a.id()),"DUPLICATE_STRATEGY","protocol.json","Duplicate strategy "+a.id());
        Set<String> sources=new HashSet<>(); sources.add(p.document().path("provenance").path("source_id").asText());
        p.document().path("extensions").path("sources").forEach(s->sources.add(s.path("source_id").asText()));
        c.test(!d.units().isEmpty(),"EMPTY_UNITS","units.tsv","At least one unit required");
        for(ExperimentalUnit u:d.units()) c.test(units.putIfAbsent(u.unitId(),u)==null,"DUPLICATE_UNIT",u.unitId(),"Duplicate unit_id");
        for(VariableDefinition v:d.variables()) {
            c.test(variables.putIfAbsent(v.variableId(),v)==null,"DUPLICATE_VARIABLE",v.variableId(),"Duplicate variable_id");
            c.test(Set.of("baseline_confounder","time_varying_confounder","outcome","mediator","effect_modifier","precision_covariate","descriptive").contains(v.role()),"VARIABLE_ROLE",v.variableId(),"Unknown variable role");
            c.test(Set.of("number","integer","boolean","string").contains(v.dataType()),"VARIABLE_TYPE",v.variableId(),"Unknown data type");
            c.test(!Set.of("number","integer").contains(v.dataType())||!v.unit().equals("NA"),"VARIABLE_UNIT",v.variableId(),"Numeric variables require unit");
            c.test(v.missingCodes().equals("NA"),"MISSING_CODES",v.variableId(),"v1.0 core supports only NA; other codes require explicit normalization");
        }
        c.test(variables.containsKey(t.outcome().variableId()),"OUTCOME_VARIABLE","protocol.json","Primary outcome missing from dictionary");
        if(variables.containsKey(t.outcome().variableId())) {
            var v=variables.get(t.outcome().variableId());
            c.test(v.role().equals("outcome"),"OUTCOME_ROLE",v.variableId(),"Primary outcome must have outcome role");
            c.test(v.unit().equals(t.outcome().unit()),"OUTCOME_UNIT",v.variableId(),"Protocol and dictionary units differ");
        }
        c.test(t.outcome().time().compareTo(t.followupEnd())<=0,"OUTCOME_HORIZON","protocol.json","Outcome exceeds follow-up");
        String[] contrast=t.estimand().contrast().split("-",-1);
        c.test(contrast.length==2 && strategies.contains(contrast[0]) && strategies.contains(contrast[1]) && !contrast[0].equals(contrast[1]),"CONTRAST","protocol.json","v1.0 core requires two distinct strategy IDs in A-B form");
        for(ExperimentalUnit u:d.units()) {
            String at=u.unitId();c.trial(u.trialId(),at);
            c.test(!u.unitId().equals("NA"),"UNIT_ID",at,"unit_id cannot be NA");
            c.test(Set.of("plant","pot","plot","chamber","site").contains(u.unitType()),"UNIT_TYPE",at,"Unknown unit type");
            c.test(strategies.contains(u.strategyId()),"STRATEGY_FK",at,"Undefined strategy");
            c.test(sources.contains(u.sourceId()),"SOURCE_FK",at,"Undefined source");
            c.test(u.parentUnitId().equals("NA")||units.containsKey(u.parentUnitId()),"PARENT_FK",at,"Unknown parent");
            var assigned=units.get(u.assignmentUnitId());
            c.test(assigned!=null,"ASSIGNMENT_FK",at,"Unknown assignment unit");
            if(assigned!=null) {
                c.test(assigned.unitType().equals(t.assignmentUnit()),"ASSIGNMENT_LEVEL",at,"Assignment unit type differs from protocol");
                c.test(assigned.strategyId().equals(u.strategyId()),"SHARED_ASSIGNMENT",at,"Nested units disagree on assigned strategy");
            }
            Set<String> seen=new HashSet<>();String current=at;
            while(units.containsKey(current)) {if(!seen.add(current)) {c.test(false,"PARENT_CYCLE",at,"Cycle in unit hierarchy");break;} current=units.get(current).parentUnitId();}
            if(u.timeZero().equals("NA")) c.warn(true,"UNKNOWN_CALENDAR_ZERO",at,"Relative origin retained; calendar start unknown");
            else {
                boolean ok=false;
                try {LocalDate.parse(u.timeZero());ok=true;} catch(Exception ignored) {}
                if(!ok) try {OffsetDateTime.parse(u.timeZero());ok=true;} catch(Exception ignored) {}
                c.test(ok,"TIME_ZERO",at,"Use ISO date or timestamp with UTC offset");
            }
            c.warn(u.clusterId().equals("NA"),"UNKNOWN_CLUSTER",at,"Inference cluster requires design review");
        }
        Set<List<String>> observationKeys=new HashSet<>();Set<String> endpoints=new HashSet<>();
        for(Observation o:d.observations()) {
            String at=o.unitId()+":"+o.variableId()+":"+o.time();c.trial(o.trialId(),at);
            c.test(units.containsKey(o.unitId()),"OBS_UNIT_FK",at,"Unknown unit");
            c.test(sources.contains(o.sourceId()),"SOURCE_FK",at,"Undefined source");
            var v=variables.get(o.variableId());c.test(v!=null,"OBS_VARIABLE_FK",at,"Unknown variable");
            c.test(observationKeys.add(List.of(o.unitId(),o.time().stripTrailingZeros().toPlainString(),o.variableId(),o.sampleId(),o.technicalReplicateId())),"DUPLICATE_OBSERVATION",at,"Duplicate observation key");
            if(v!=null) {
                c.test(!v.role().equals("baseline_confounder")||o.time().signum()<=0,"POST_BASELINE",at,"Baseline confounder measured after treatment");
                c.warn(v.role().equals("baseline_confounder")&&o.time().signum()==0,"BASELINE_ORDER",at,"Confirm measurement preceded first treatment");
                c.test(!v.role().equals("outcome")||o.time().signum()>=0,"PRE_OUTCOME",at,"Outcome before time zero");
                c.test(o.time().compareTo(t.followupEnd())<=0,"OBS_HORIZON",at,"Observation beyond defined follow-up");
                if(!o.value().equals("NA")) {
                    boolean valid=true;
                    try {switch(v.dataType()) {case "number" -> new BigDecimal(o.value());case "integer" -> new BigDecimal(o.value()).toBigIntegerExact();case "boolean" -> {valid=Set.of("0","1").contains(o.value());} default -> {}}} catch(Exception ex) {valid=false;}
                    c.test(valid,"VALUE_TYPE",at,"Value differs from declared type; NaN/Infinity disallowed");
                    c.test(o.missingReason().equals("NA"),"UNEXPECTED_MISSING_REASON",at,"Nonmissing value must use NA reason");
                }
            }
            if(o.value().equals("NA")) c.test(Set.of("not_measured","lost_to_followup","below_detection","not_applicable","unknown").contains(o.missingReason()),"MISSING_REASON",at,"Missing value requires recognized reason");
            if(o.variableId().equals(t.outcome().variableId())&&o.time().compareTo(t.outcome().time())==0) endpoints.add(o.unitId());
        }
        for(var u:d.units()) if(u.eligible()&&u.unitType().equals(t.analysisUnit())) c.test(endpoints.contains(u.unitId()),"MISSING_ENDPOINT",u.unitId(),"Primary endpoint row required, including explicit missing values");
        Map<String,List<TreatmentInterval>> intervals=new TreeMap<>();
        for(var a:d.treatments()) {
            String at=a.assignmentUnitId();c.trial(a.trialId(),at);
            var u=units.get(at);c.test(u!=null,"TREATMENT_UNIT_FK",at,"Unknown unit");
            if(u!=null) c.test(u.assignmentUnitId().equals(at)&&u.unitType().equals(t.assignmentUnit()),"TREATMENT_LEVEL",at,"Exposure must be recorded at allocation level");
            c.test(strategies.contains(a.strategyId()),"TREATMENT_STRATEGY_FK",at,"Unknown strategy");
            c.test(sources.contains(a.sourceId()),"SOURCE_FK",at,"Undefined source");
            c.test(a.start().signum()>=0&&a.stop().compareTo(a.start())>0&&a.stop().compareTo(t.followupEnd())<=0,"INTERVAL",at,"Require 0 <= start < stop <= followup_end");
            c.test(Set.of("adherent","nonadherent","unknown").contains(a.adherence()),"ADHERENCE",at,"Unknown adherence code");
            c.test(a.dose().equals("NA")==a.doseUnit().equals("NA"),"DOSE_UNIT",at,"Dose and unit must both be specified or both NA");
            if(!a.dose().equals("NA")) try {c.test(new BigDecimal(a.dose()).signum()>=0,"DOSE",at,"Negative dose");} catch(Exception ex) {c.test(false,"DOSE",at,"Invalid numeric dose");}
            intervals.computeIfAbsent(at,k->new ArrayList<>()).add(a);
        }
        intervals.forEach((id,rows)->{rows.sort(Comparator.comparing(TreatmentInterval::start));for(int i=1;i<rows.size();i++) c.test(rows.get(i-1).stop().compareTo(rows.get(i).start())<=0,"OVERLAP",id,"Overlapping exposure intervals");});
        if(t.estimand().strategyEffect().equals("sustained_strategy")) {
            Set<String> assigned=new TreeSet<>(); d.units().stream().filter(ExperimentalUnit::eligible).forEach(u->assigned.add(u.assignmentUnitId()));
            for(String id:assigned) {
                var rows=intervals.getOrDefault(id,List.of());BigDecimal cursor=BigDecimal.ZERO;boolean covered=true;
                for(var a:rows) {if(a.start().compareTo(cursor)!=0) covered=false;cursor=a.stop();}
                covered=covered&&!rows.isEmpty()&&cursor.compareTo(t.followupEnd())==0;
                c.warn(!covered,"EXPOSURE_COVERAGE",id,"Sustained strategy exposure history incomplete");
                c.warn(rows.stream().anyMatch(a->a.adherence().equals("unknown")),"UNKNOWN_ADHERENCE",id,"No verified adherence evidence");
            }
        }
        for(var e:d.events()) {
            c.trial(e.trialId(),e.unitId());c.test(units.containsKey(e.unitId()),"EVENT_UNIT_FK",e.unitId(),"Unknown event unit");
            c.test(sources.contains(e.sourceId()),"SOURCE_FK",e.unitId(),"Undefined source");
            c.test(e.time().signum()>=0&&e.time().compareTo(t.followupEnd())<=0,"EVENT_TIME",e.unitId(),"Event outside follow-up");
            c.test(Set.of("death","loss_to_followup","harvest","strategy_switch","other").contains(e.eventType()),"EVENT_TYPE",e.unitId(),"Unknown event type");
        }
        c.warn(true,"DESIGN_REVIEW_REQUIRED","protocol.json","Assess identification, time alignment, clustering, selection, interference and missingness before estimation");
        return report(out,d.units().size(),d.observations().size());
    }
}
