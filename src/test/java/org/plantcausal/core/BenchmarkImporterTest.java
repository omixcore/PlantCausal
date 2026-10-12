package org.plantcausal.core;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.ss.usermodel.*;
import java.nio.file.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkImporterTest {
    @TempDir Path dir;
    @Test void pineImportReconcilesEveryEndpointAndPreservesMissingness() throws Exception {
        Path src=Path.of("benchmark/raw/10100580/Mycorrhiza_exposure_drought.xlsx");new BenchmarkImporter().importPine(src,dir);
        var reader=new PackageReader();var d=reader.read(dir,reader.readProtocol(dir));
        assertEquals(280,d.units().size());assertEquals(1400,d.observations().size());assertTrue(d.treatments().isEmpty());
        try(var w=WorkbookFactory.create(src.toFile(),null,true)) {
            Sheet s=w.getSheet("Measurements");int[] columns={12,11,13,14};
            for(int row=1;row<=280;row++) for(int j=0;j<4;j++) {
                var obs=d.observations().get((row-1)*5+j);Cell source=s.getRow(row).getCell(columns[j]);
                assertEquals(s.getRow(row).getCell(0).getStringCellValue(),obs.unitId());
                if(source==null||source.getCellType()==CellType.BLANK) {assertEquals("NA",obs.value());assertEquals("unknown",obs.missingReason());}
                else assertEquals(0,new BigDecimal(Double.toString(source.getNumericCellValue())).compareTo(new BigDecimal(obs.value())));
            }
        }
        var r=new ValidationService().validate(dir);assertTrue(r.schemaValid(),r.issues().toString());assertEquals("NOT_ASSESSED",r.analysisReady());
        assertTrue(r.issues().stream().anyMatch(i->i.code().equals("UNKNOWN_CLUSTER")));
    }
    @Test void alteredSourceRejectedBeforeImport() throws Exception {
        var bad=dir.resolve("bad.xlsx");Files.writeString(bad,"altered");
        assertThrows(java.io.IOException.class,()->new BenchmarkImporter().importPine(bad,dir.resolve("output")));
        assertFalse(Files.exists(dir.resolve("output")));
    }
    @Test void grassImportPreservesRowsAndMissingValues() throws Exception {
        new BenchmarkImporter().stageGrass(Path.of("benchmark/raw/4306840/181004_Biomass_2014.xls"),dir);
        var lines=Files.readAllLines(dir.resolve("harvests.staging.tsv"));assertTrue(lines.size()>1000);assertTrue(lines.get(1).contains("2014-04-16"));
    }
}
