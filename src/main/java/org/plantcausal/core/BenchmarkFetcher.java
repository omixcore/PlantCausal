package org.plantcausal.core;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

public final class BenchmarkFetcher {
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private byte[] get(String url) throws Exception {
        var r=http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        if(r.statusCode()!=200) throw new java.io.IOException("Download HTTP status "+r.statusCode());return r.body();
    }
    public void fetch(Path root) throws Exception {
        for(String record:List.of("10100580","4306840")) {
            Path dest=root.resolve(record);Files.createDirectories(dest);
            byte[] metadata=get("https://zenodo.org/api/records/"+record);var node=PackageReader.JSON.readTree(metadata);
            if(!node.path("metadata").path("license").path("id").asText().equals("cc-by-4.0")) throw new java.io.IOException("License changed; manual review required");
            Files.write(dest.resolve("record.json"),metadata);
            for(var file:node.path("files")) {
                String key=file.path("key").asText();
                if(!(record.equals("10100580")||key.equals("181004_Biomass_2014.xls"))) continue;
                if(!Set.of("Mycorrhiza_exposure_drought.xlsx","Transpiration_WP_timeseries.xlsx","181004_Biomass_2014.xls").contains(key)) throw new java.io.IOException("Unexpected source filename");
                byte[] data=get(file.path("links").path("self").asText());
                String hash="md5:"+HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(data));
                if(!hash.equals(file.path("checksum").asText())) throw new java.io.IOException("Repository checksum mismatch");
                Files.write(dest.resolve(key),data);
            }
        }
    }
}
