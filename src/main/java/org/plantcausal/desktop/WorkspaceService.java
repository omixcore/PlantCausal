package org.plantcausal.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import org.apache.commons.csv.*;
import org.plantcausal.core.*;
import org.plantcausal.core.Models.ValidationReport;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** GUI file operations kept independent of Swing for testing and future front ends. */
public final class WorkspaceService {
    public static final List<String> FILES=List.of("protocol.json","units.tsv","variables.tsv","observations.tsv","treatments.tsv","events.tsv");
    public record Preview(List<String> columns,List<List<String>> rows,int totalRows) {}
    public record Snapshot(Path directory,String fingerprint,ValidationReport report) {}
    public Path builtin(String id) throws IOException {
        if(!Set.of("synthetic","PC_B001").contains(id)) throw new IOException("Unknown built-in project");
        Path dest=Files.createTempDirectory("plantcausal-"+id+"-");
        for(String file:FILES) try(var in=WorkspaceService.class.getResourceAsStream("/builtin/"+id+"/"+file)) {
            if(in==null) throw new IOException("Built-in file missing: "+file);
            Files.copy(in,dest.resolve(file));
        }
        return dest;
    }
    public JsonNode protocol(Path directory) throws IOException {return new PackageReader().readProtocol(directory);}
    public JsonNode checkProtocol(String text) throws Exception {
        JsonNode node=PackageReader.JSON.readTree(text);
        try(var in=WorkspaceService.class.getResourceAsStream("/schema/protocol.schema.json")) {
            var schema=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(PackageReader.JSON.readTree(in));
            var issues=schema.validate(node);
            if(!issues.isEmpty()) throw new IOException("Protocol schema errors:\n"+String.join("\n",issues.stream().map(Object::toString).sorted().toList()));
        }
        return node;
    }
    public void saveProtocol(Path directory,String text) throws Exception {
        JsonNode node=checkProtocol(text);
        Path temp=Files.createTempFile(directory,".protocol-",".json");
        try {
            Files.writeString(temp,PackageReader.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(node)+"\n",StandardCharsets.UTF_8);
            try {Files.move(temp,directory.resolve("protocol.json"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex) {Files.move(temp,directory.resolve("protocol.json"),StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }
    public void saveProjectAs(Path source,Path destination,String text) throws Exception {
        checkProtocol(text);copyProject(source,destination);saveProtocol(destination,text);
    }
    public void copyProject(Path source,Path destination) throws IOException {
        Path src=source.toRealPath(),dest=destination.toAbsolutePath().normalize();
        if(src.equals(dest)||dest.startsWith(src)) throw new IOException("Choose a different folder outside the current project");
        Files.createDirectories(dest);
        try(var files=Files.list(dest)) {if(files.findAny().isPresent()) throw new IOException("Destination must be an empty folder");}
        for(String name:FILES) if(Files.isRegularFile(src.resolve(name))) Files.copy(src.resolve(name),dest.resolve(name));
    }
    public Preview preview(Path file,int limit) throws IOException {
        try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8);
            var parser=CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            List<List<String>> rows=new ArrayList<>();int total=0;
            for(CSVRecord record:parser) {
                if(!record.isConsistent()) throw new IOException("Inconsistent TSV row: "+record.getRecordNumber());
                total++;if(rows.size()<limit) rows.add(List.copyOf(record.toList()));
            }
            return new Preview(List.copyOf(parser.getHeaderNames()),List.copyOf(rows),total);
        }
    }
    public String fingerprint(Path directory) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(String name:FILES) {
            digest.update(name.getBytes(StandardCharsets.UTF_8));Path file=directory.resolve(name);
            digest.update((byte)(Files.isRegularFile(file)?1:0));
            if(Files.isRegularFile(file)) try(var in=Files.newInputStream(file)) {
                byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    public Snapshot validate(Path directory) throws Exception {
        String before=fingerprint(directory);ValidationReport report=new ValidationService().validate(directory);
        if(!before.equals(fingerprint(directory))) throw new IOException("Project changed during validation; run again");
        return new Snapshot(directory.toAbsolutePath().normalize(),before,report);
    }
    public void exportReport(Snapshot snapshot,Path target) throws Exception {
        if(!snapshot.fingerprint().equals(fingerprint(snapshot.directory()))) throw new IOException("Project changed since validation. Validate again before export.");
        var result=PackageReader.JSON.createObjectNode();result.put("input_sha256",snapshot.fingerprint());
        result.set("validation",PackageReader.JSON.valueToTree(snapshot.report()));
        Files.writeString(target,PackageReader.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result)+"\n",StandardCharsets.UTF_8);
    }
}
