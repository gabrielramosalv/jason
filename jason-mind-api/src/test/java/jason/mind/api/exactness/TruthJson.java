package jason.mind.api.exactness;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class TruthJson {

    private TruthJson() {
    }

    static String write(Map<String, ExactnessOracle.AgentTruth> truth) {
        JsonObjectBuilder agents = Json.createObjectBuilder();
        for (ExactnessOracle.AgentTruth t : truth.values()) {
            JsonObjectBuilder cycles = Json.createObjectBuilder();
            for (Map.Entry<Integer, ExactnessOracle.CycleTruth> e : new TreeMap<>(t.cycles).entrySet()) {
                ExactnessOracle.CycleTruth c = e.getValue();
                cycles.add(String.valueOf(e.getKey()), Json.createObjectBuilder()
                        .add("beliefs", array(c.beliefs)).add("intentions", array(c.intentions)).add("events", array(c.events))
                        .add("plans", c.plans).add("selectedEvents", array(c.selectedEvents)).add("chosenPlans", array(c.chosenPlans))
                        .add("selectedIntentions", array(c.selectedIntentions)).add("executions", c.executions)
                        .add("consoleLines", c.consoleLines).add("messagesSent", c.messagesSent));
            }
            List<String> sent;
            synchronized (t.sent) {
                sent = List.copyOf(t.sent);
            }
            agents.add(t.name, Json.createObjectBuilder().add("lastCycle", t.lastCycle).add("sent", array(sent)).add("cycles", cycles));
        }
        return agents.build().toString();
    }

    static Map<String, ExactnessOracle.AgentTruth> read(String json) {
        Map<String, ExactnessOracle.AgentTruth> r = new TreeMap<>();
        JsonObject root;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            root = reader.readObject();
        }
        for (Map.Entry<String, JsonValue> a : root.entrySet()) {
            JsonObject o = a.getValue().asJsonObject();
            ExactnessOracle.AgentTruth t = new ExactnessOracle.AgentTruth(a.getKey());
            t.lastCycle = o.getInt("lastCycle");
            for (JsonValue v : o.getJsonArray("sent")) t.sent.add(((JsonString) v).getString());
            for (Map.Entry<String, JsonValue> c : o.getJsonObject("cycles").entrySet()) {
                JsonObject x = c.getValue().asJsonObject();
                ExactnessOracle.CycleTruth ct = new ExactnessOracle.CycleTruth();
                fill(ct.beliefs, x, "beliefs");
                fill(ct.intentions, x, "intentions");
                fill(ct.events, x, "events");
                fill(ct.selectedEvents, x, "selectedEvents");
                fill(ct.chosenPlans, x, "chosenPlans");
                fill(ct.selectedIntentions, x, "selectedIntentions");
                ct.plans = x.getInt("plans");
                ct.executions = x.getInt("executions");
                ct.consoleLines = x.getInt("consoleLines");
                ct.messagesSent = x.getInt("messagesSent");
                t.cycles.put(Integer.parseInt(c.getKey()), ct);
            }
            r.put(t.name, t);
        }
        return r;
    }

    private static JsonArrayBuilder array(List<String> l) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        for (String s : l) b.add(s);
        return b;
    }

    private static void fill(List<String> target, JsonObject o, String key) {
        for (JsonValue v : o.getJsonArray(key)) target.add(((JsonString) v).getString());
    }
}
