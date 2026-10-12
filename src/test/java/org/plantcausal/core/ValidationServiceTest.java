package org.plantcausal.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {
    @TempDir Path dir;
    private void fixture() throws Exception {
        try(var files=Files.list(Path.of("examples/synthetic"))) {
            for(Path f:files.toList()) Files.copy(f,dir.resolve(f.getFileName()));
        }
    }
    private void change(String name,String field,String value) throws Exception {
        Path file=dir.resolve(name);var rows=new ArrayList<>(Files.readAllLines(file));
        var headers=Arrays.asList(rows.get(0).split("\t",-1));int index=headers.indexOf(field);assertTrue(index>=0);
        var data=rows.get(1).split("\t",-1);data[index]=value;rows.set(1,String.join("\t",data));Files.write(file,rows);
    }
    @Test void validPackageNeverClaimsCausality() throws Exception {
        fixture();var report=new ValidationService().validate(dir);assertTrue(report.schemaValid(),report.issues().toString());
        assertEquals("NOT_ASSESSED",report.analysisReady());assertEquals(4,report.unitCount());assertEquals(8,report.observationCount());
    }
    @ParameterizedTest @CsvSource({
        "observations.tsv,unit_id,UNKNOWN,OBS_UNIT_FK",
        "observations.tsv,variable_id,UNKNOWN,OBS_VARIABLE_FK",
        "observations.tsv,time,1,POST_BASELINE",
        "observations.tsv,value,NaN,VALUE_TYPE",
        "observations.tsv,value,Infinity,VALUE_TYPE",
        "observations.tsv,value,NA,MISSING_REASON",
        "observations.tsv,source_id,UNKNOWN,SOURCE_FK",
        "observations.tsv,trial_id,UNKNOWN,TRIAL_ID",
        "units.tsv,parent_unit_id,P1,PARENT_CYCLE",
        "units.tsv,parent_unit_id,UNKNOWN,PARENT_FK",
        "units.tsv,assignment_unit_id,UNKNOWN,ASSIGNMENT_FK",
        "units.tsv,unit_type,pot,ASSIGNMENT_LEVEL",
        "units.tsv,strategy_id,UNKNOWN,STRATEGY_FK",
        "units.tsv,time_zero,2026-02-30,TIME_ZERO",
        "variables.tsv,role,confounder,VARIABLE_ROLE",
        "variables.tsv,unit,NA,VARIABLE_UNIT",
        "variables.tsv,data_type,float,VARIABLE_TYPE",
        "treatments.tsv,start,-1,INTERVAL",
        "treatments.tsv,stop,0,INTERVAL",
        "treatments.tsv,stop,91,INTERVAL",
        "treatments.tsv,dose,3,DOSE_UNIT",
        "treatments.tsv,adherence,yes,ADHERENCE"
    }) void rejectsInvalidFields(String file,String field,String value,String code) throws Exception {
        fixture();change(file,field,value);var r=new ValidationService().validate(dir);
        assertFalse(r.schemaValid());assertTrue(r.issues().stream().anyMatch(i->i.code().equals(code)),r.issues().toString());
    }
    @Test void duplicateRowsAndOverlapsRejected() throws Exception {
        fixture();for(String file:List.of("units.tsv","observations.tsv","treatments.tsv")) {
            var rows=Files.readAllLines(dir.resolve(file));Files.writeString(dir.resolve(file),rows.get(1)+"\n",StandardOpenOption.APPEND);
        }
        var r=new ValidationService().validate(dir);for(String code:List.of("DUPLICATE_UNIT","DUPLICATE_OBSERVATION","OVERLAP")) assertTrue(r.issues().stream().anyMatch(i->i.code().equals(code)));
    }
    @Test void schemaRejectsUnknownProtocolAndMissingRequiredField() throws Exception {
        fixture();var x=(com.fasterxml.jackson.databind.node.ObjectNode)PackageReader.JSON.readTree(dir.resolve("protocol.json").toFile());
        x.put("unknown",1);x.remove("identification");PackageReader.JSON.writeValue(dir.resolve("protocol.json").toFile(),x);
        var r=new ValidationService().validate(dir);assertFalse(r.schemaValid());assertTrue(r.issues().stream().allMatch(i->i.code().equals("PROTOCOL_SCHEMA")));
    }
    @Test void duplicateJsonKeysRejected() throws Exception {
        fixture();var f=dir.resolve("protocol.json");Files.writeString(f,Files.readString(f).replaceFirst("\\{","{\"trial_id\":\"duplicate\","));
        assertFalse(new ValidationService().validate(dir).schemaValid());
    }
    @Test void parserRejectsDuplicateHeaderAndShortRow() throws Exception {
        fixture();var f=dir.resolve("observations.tsv");Files.writeString(f,Files.readString(f).replaceFirst("variable_id","unit_id"));
        var r=new ValidationService().validate(dir);assertFalse(r.schemaValid());assertEquals("PACKAGE_PARSE",r.issues().get(0).code());
    }
    @Test void missingFilesYieldReportInsteadOfCrash() {assertFalse(new ValidationService().validate(dir).schemaValid());}
    @Test void emptyExposureHistoryWarnsWithoutInventingAdherence() throws Exception {
        fixture();var f=dir.resolve("treatments.tsv");Files.writeString(f,Files.readAllLines(f).get(0)+"\n");
        var r=new ValidationService().validate(dir);assertTrue(r.schemaValid());assertTrue(r.issues().stream().anyMatch(i->i.code().equals("EXPOSURE_COVERAGE")));
    }
    @Test void missingEndpointIsError() throws Exception {
        fixture();var f=dir.resolve("observations.tsv");var lines=new ArrayList<>(Files.readAllLines(f));lines.remove(2);Files.write(f,lines);
        assertTrue(new ValidationService().validate(dir).issues().stream().anyMatch(i->i.code().equals("MISSING_ENDPOINT")));
    }
    @Test void tsvQuotesAreParsedAndReportIsDeterministic() throws Exception {
        fixture();var f=dir.resolve("variables.tsv");Files.writeString(f,Files.readString(f).replace("基线土壤含水量","\"基线\t含水量\""));
        var service=new ValidationService();assertTrue(service.validate(dir).schemaValid());assertEquals(service.validate(dir),service.validate(dir));
    }
}
