package org.plantcausal.core;

import org.apache.poi.ss.usermodel.*;
import org.apache.commons.csv.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Reproducible adapters for specifically reviewed, checksum-pinned source releases. */
public final class BenchmarkImporter {
    private static final String PINE_HASH="d6604bbb852fee22cf282f1a3f805f3d2a680aad98cd2b88c60bb54a64f454a1";
    private static final String GRASS_HASH="a87d0bfa5cd59b0204667aba234306366566376c3811284630e2bc01f8588546";
    private static final String TRIAL="PC_B001",SOURCE="ZENODO_10100580";
    private void verify(Path source,String expected) throws Exception {
        String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
        if(!actual.equals(expected)) throw new IOException("Source checksum mismatch: adapter requires the reviewed release");
    }
    private String value(Cell cell) {
        if(cell==null||cell.getCellType()==CellType.BLANK) return "NA";
        CellType type=cell.getCellType()==CellType.FORMULA?cell.getCachedFormulaResultType():cell.getCellType();
        if(type==CellType.NUMERIC) return Double.toString(cell.getNumericCellValue());
        if(type==CellType.STRING) return cell.getStringCellValue().trim().isEmpty()?"NA":cell.getStringCellValue().trim();
        throw new IllegalArgumentException("Unsupported Excel cell type: "+cell.getCellType());
    }
    private CSVPrinter printer(Path file,String... header) throws IOException {
        return new CSVPrinter(Files.newBufferedWriter(file,StandardCharsets.UTF_8),CSVFormat.TDF.builder().setHeader(header).build());
    }
    public void importPine(Path source,Path output) throws Exception {
        verify(source,PINE_HASH);Files.createDirectories(output);
        try(var input=BenchmarkImporter.class.getResourceAsStream("/benchmark/pine.protocol.json")) {Files.copy(input,output.resolve("protocol.json"),StandardCopyOption.REPLACE_EXISTING);}
        try(var vars=printer(output.resolve("variables.tsv"),"variable_id","role","data_type","unit","definition","timing","missing_codes")) {
            vars.printRecord("transpiration","outcome","number","mgH2O/g_needle_dry_biomass/h","Water loss per dry needle biomass per hour","day10_and_day24","NA");
            vars.printRecord("water_potential","outcome","number","MPa","Needle water potential","day10_and_day24","NA");
            vars.printRecord("emf_group","effect_modifier","string","NA","Pre-existing EMF or soil inoculum exposure group","before_drought_start","NA");
        }
        try(var units=printer(output.resolve("units.tsv"),"trial_id","unit_id","parent_unit_id","unit_type","assignment_unit_id","cluster_id","block_id","species","genotype","time_zero","eligible","strategy_id","source_id");
            var obs=printer(output.resolve("observations.tsv"),"trial_id","unit_id","time","variable_id","value","missing_reason","sample_id","technical_replicate_id","source_id","source_location");
            var workbook=WorkbookFactory.create(source.toFile(),null,true)) {
            Sheet sheet=workbook.getSheet("Measurements");Map<String,Integer> columns=new HashMap<>();
            for(Cell cell:sheet.getRow(0)) columns.put(value(cell),cell.getColumnIndex());
            for(String field:List.of("code","strain","treat","trans_10_bio","trans_24_bio","wp_10d","wp_24d")) if(!columns.containsKey(field)) throw new IOException("Source column missing: "+field);
            Set<String> ids=new HashSet<>();
            for(int i=1;i<=sheet.getLastRowNum();i++) {
                Row row=sheet.getRow(i);String id=value(row.getCell(columns.get("code"))),strategy=value(row.getCell(columns.get("treat")));
                if(id.equals("NA")||!ids.add(id)||!Set.of("D","M").contains(strategy)) throw new IOException("Invalid source ID/treatment at row "+(i+1));
                units.printRecord(TRIAL,id,"NA","pot",id,"NA","NA","Pinus sylvestris","NA","NA","1",strategy,SOURCE);
                for(String[] field:List.of(new String[]{"trans_10_bio","transpiration","10"},new String[]{"trans_24_bio","transpiration","24"},new String[]{"wp_10d","water_potential","10"},new String[]{"wp_24d","water_potential","24"},new String[]{"strain","emf_group","0"})) {
                    String v=value(row.getCell(columns.get(field[0])));
                    obs.printRecord(TRIAL,id,field[2],field[1],v,v.equals("NA")?"unknown":"NA","NA","NA",SOURCE,"Mycorrhiza_exposure_drought.xlsx!Measurements!row="+(i+1)+";column="+field[0]);
                }
            }
        }
        try(var ignored=printer(output.resolve("treatments.tsv"),"trial_id","assignment_unit_id","start","stop","strategy_id","exposure_name","dose","dose_unit","adherence","source_id")) {}
        try(var ignored=printer(output.resolve("events.tsv"),"trial_id","unit_id","time","event_type","reason","source_id")) {}
    }
    public void stageGrass(Path source,Path output) throws Exception {
        verify(source,GRASS_HASH);Files.createDirectories(output);
        try(var book=WorkbookFactory.create(source.toFile(),null,true);var table=printer(output.resolve("harvests.staging.tsv"),"plot_id","cultivar","treatment","series","replicate","harvest","date","dry_weight_kg_ha","source_row")) {
            Sheet s=book.getSheet("Data");
            if(!value(s.getRow(15).getCell(0)).equals("Plot")) throw new IOException("Unexpected grass workbook layout");
            for(int i=16;i<=s.getLastRowNum();i++) {
                Row r=s.getRow(i);if(r==null||value(r.getCell(1)).equals("NA")) continue;
                String date="NA";Cell d=r.getCell(6);
                if(d!=null&&d.getCellType()==CellType.NUMERIC) date=DateUtil.getLocalDateTime(d.getNumericCellValue()).toLocalDate().toString();
                table.printRecord(value(r.getCell(0)),value(r.getCell(1)),value(r.getCell(2)),value(r.getCell(3)),value(r.getCell(4)),value(r.getCell(5)),date,value(r.getCell(10)),i+1);
            }
        }
    }
}
