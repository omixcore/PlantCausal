package org.plantcausal.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import org.apache.commons.csv.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import static org.plantcausal.core.Models.*;

public final class PackageReader {
    public static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    static String s(JsonNode x, String key) { return x.path(key).asText(); }
    public JsonNode readProtocol(Path dir) throws IOException { return JSON.readTree(dir.resolve("protocol.json").toFile()); }
    public Protocol protocol(JsonNode x) {
        JsonNode t=x.get("target_trial"), o=t.get("outcome"), e=t.get("estimand");
        List<Strategy> strategies=new ArrayList<>();
        t.get("strategies").forEach(a->strategies.add(new Strategy(s(a,"id"),s(a,"description"))));
        return new Protocol(s(x,"schema_version"),s(x,"trial_id"),s(x,"data_kind"),
            new TargetTrial(s(t,"population"),s(t,"eligibility"),s(t,"assignment_unit"),s(t,"analysis_unit"),s(t,"assignment"),s(t,"time_zero"),s(t,"time_unit"),t.get("followup_end").decimalValue(),List.copyOf(strategies),
            new Outcome(s(o,"variable_id"),o.get("time").decimalValue(),s(o,"unit"),s(o,"definition")),
            new Estimand(s(e,"population"),s(e,"contrast"),s(e,"effect"),s(e,"strategy_effect"),s(e,"intercurrent_events")),s(t,"analysis_plan")),x.deepCopy());
    }
    private List<Map<String,String>> rows(Path dir,String name,String... columns) throws IOException {
        try (Reader reader=Files.newBufferedReader(dir.resolve(name),StandardCharsets.UTF_8);
             CSVParser parser=CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(false).build().parse(reader)) {
            var headers=parser.getHeaderNames();
            if (headers.size()!=new HashSet<>(headers).size()) throw new IOException(name+": duplicate headers");
            if (!new HashSet<>(headers).equals(Set.of(columns))) throw new IOException(name+": expected exact columns "+Arrays.toString(columns));
            List<Map<String,String>> result=new ArrayList<>();
            for(CSVRecord row:parser) {
                if(!row.isConsistent()) throw new IOException(name+": inconsistent row "+row.getRecordNumber());
                Map<String,String> values=row.toMap();
                for (var item:values.entrySet()) if(item.getValue().isBlank()) throw new IOException(name+": empty "+item.getKey()+" at row "+row.getRecordNumber()+"; use NA");
                result.add(values);
            }
            return result;
        }
    }
    private static BigDecimal n(Map<String,String> r,String k) {return new BigDecimal(r.get(k));}
    public DataPackage read(Path dir,JsonNode x) throws IOException {
        var units=rows(dir,"units.tsv","trial_id","unit_id","parent_unit_id","unit_type","assignment_unit_id","cluster_id","block_id","species","genotype","time_zero","eligible","strategy_id","source_id").stream().map(r->{
            if(!Set.of("0","1").contains(r.get("eligible"))) throw new IllegalArgumentException("eligible must be 0/1");
            return new ExperimentalUnit(r.get("trial_id"),r.get("unit_id"),r.get("parent_unit_id"),r.get("unit_type"),r.get("assignment_unit_id"),r.get("cluster_id"),r.get("block_id"),r.get("species"),r.get("genotype"),r.get("time_zero"),r.get("eligible").equals("1"),r.get("strategy_id"),r.get("source_id"));}).toList();
        var variables=rows(dir,"variables.tsv","variable_id","role","data_type","unit","definition","timing","missing_codes").stream().map(r->new VariableDefinition(r.get("variable_id"),r.get("role"),r.get("data_type"),r.get("unit"),r.get("definition"),r.get("timing"),r.get("missing_codes"))).toList();
        var obs=rows(dir,"observations.tsv","trial_id","unit_id","time","variable_id","value","missing_reason","sample_id","technical_replicate_id","source_id","source_location").stream().map(r->new Observation(r.get("trial_id"),r.get("unit_id"),n(r,"time"),r.get("variable_id"),r.get("value"),r.get("missing_reason"),r.get("sample_id"),r.get("technical_replicate_id"),r.get("source_id"),r.get("source_location"))).toList();
        var treatments=rows(dir,"treatments.tsv","trial_id","assignment_unit_id","start","stop","strategy_id","exposure_name","dose","dose_unit","adherence","source_id").stream().map(r->new TreatmentInterval(r.get("trial_id"),r.get("assignment_unit_id"),n(r,"start"),n(r,"stop"),r.get("strategy_id"),r.get("exposure_name"),r.get("dose"),r.get("dose_unit"),r.get("adherence"),r.get("source_id"))).toList();
        var events=rows(dir,"events.tsv","trial_id","unit_id","time","event_type","reason","source_id").stream().map(r->new Event(r.get("trial_id"),r.get("unit_id"),n(r,"time"),r.get("event_type"),r.get("reason"),r.get("source_id"))).toList();
        return new DataPackage(protocol(x),units,variables,obs,treatments,events);
    }
}
