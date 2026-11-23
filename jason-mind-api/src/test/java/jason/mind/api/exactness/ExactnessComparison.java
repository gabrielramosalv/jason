package jason.mind.api.exactness;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ExactnessComparison {

    public static void main(String[] args) throws Exception {
        var truth = TruthJson.read(Files.readString(Path.of(args[0])));
        String base = args.length > 2 ? args[2] : "http://127.0.0.1:3272";
        keepEverything(base);
        ExactnessOracle.Comparison c = new ExactnessOracle.Comparison(base, truth);
        Files.writeString(Path.of(args[1]), c.run());
        System.out.println("ORACLE " + c.summary());
    }

    static void keepEverything(String base) throws Exception {
        var con = (java.net.HttpURLConnection) java.net.URI.create(base + "/api/settings").toURL().openConnection(java.net.Proxy.NO_PROXY);
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.getOutputStream().write("{\"retentionCycles\": 10000000}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("PUT /api/settings " + con.getResponseCode());
        con.getInputStream().readAllBytes();
    }
}
