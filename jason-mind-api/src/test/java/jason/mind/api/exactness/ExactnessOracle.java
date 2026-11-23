package jason.mind.api.exactness;

import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.Event;
import jason.asSemantics.IntendedMeans;
import jason.asSemantics.Intention;
import jason.asSemantics.Message;
import jason.asSemantics.ReasoningListener;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import jason.asSyntax.Plan;
import jason.asSyntax.Term;
import jason.asSyntax.Trigger;
import jason.infra.local.RunLocalMAS;
import jason.mind.api.bootstrap.MindApiManager;
import jason.util.Config;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public final class ExactnessOracle {

    static final Map<String, AgentTruth> TRUTH = new ConcurrentHashMap<>();
    static volatile int maxCycles = 400;
    static final boolean PAUSE = !"false".equals(System.getProperty("oracle.pause"));

    public static void main(String[] args) throws Exception {
        String mas = args[0];
        int seconds = Integer.parseInt(args[1]);
        Path truthFile = Path.of(args[2]);
        if (args.length > 3) maxCycles = Integer.parseInt(args[3]);
        int keepSeconds = args.length > 4 ? Integer.parseInt(args[4]) : 300;
        Config.get().setProperty(Config.MIND_INSP_WEB_SERVER_CLASS_NAME, OracleManager.class.getName());
        Thread runner = new Thread(() -> {
            try {
                RunLocalMAS.main(new String[]{mas, "--no-rmi", "--no-mbean", "--log-conf", System.getProperty("oracle.logConf", "")});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "oracle-mas");
        runner.setDaemon(true);
        runner.start();
        Thread.sleep(seconds * 1000L);
        for (AgentTruth t : TRUTH.values()) t.frozen = true;
        Thread.sleep(1500);
        Files.writeString(truthFile, TruthJson.write(TRUTH));
        Files.writeString(Path.of(truthFile + ".ready"), "ok\n");
        Thread.sleep(keepSeconds * 1000L);
        Runtime.getRuntime().halt(0);
    }

    public static final class OracleManager extends MindApiManager {
        @Override
        public void registerAg(Agent agent) {
            super.registerAg(agent);
            String name = agent.getTS().getAgArch().getAgName();
            AgentTruth truth = new AgentTruth(name);
            TRUTH.put(name, truth);
            ReasoningListener capture = agent.getTS().getReasoningListener();
            agent.getTS().setReasoningListener(new Tee(capture, truth));
            agent.getTS().getLogger().addHandler(new Handler() {
                @Override public void publish(LogRecord r) {
                    if (r != null && r.getLevel().intValue() >= Level.INFO.intValue()) truth.console(agent);
                }
                @Override public void flush() { }
                @Override public void close() { }
            });
        }
    }

    static final class CycleTruth {
        final List<String> beliefs = new ArrayList<>();
        final List<String> intentions = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        int plans;
        final List<String> selectedEvents = new ArrayList<>();
        final List<String> chosenPlans = new ArrayList<>();
        final List<String> selectedIntentions = new ArrayList<>();
        int executions;
        int consoleLines;
        int messagesSent;
    }

    static final class AgentTruth {
        final String name;
        final Map<Integer, CycleTruth> cycles = new ConcurrentHashMap<>();
        volatile boolean frozen;
        volatile int lastCycle = -1;
        int cycle;
        CycleTruth current;
        final List<String> sent = Collections.synchronizedList(new ArrayList<>());

        AgentTruth(String name) {
            this.name = name;
        }

        boolean observing() {
            return !frozen && current != null && cycle <= maxCycles;
        }

        void console(Agent ag) {
            CycleTruth c = current;
            if (c != null && !frozen) c.consoleLines++;
        }
    }

    static final class Tee implements ReasoningListener {
        private final ReasoningListener capture;
        private final AgentTruth t;

        Tee(ReasoningListener capture, AgentTruth truth) {
            this.capture = capture;
            this.t = truth;
        }

        @Override public void cycleStarting(TransitionSystem ts, int cycle) {

            while (t.frozen && PAUSE) {
                try { Thread.sleep(60_000); } catch (InterruptedException e) { return; }
            }
            if (capture != null) capture.cycleStarting(ts, cycle);
            t.cycle = cycle;
            t.current = t.frozen || cycle > maxCycles ? null : new CycleTruth();
        }

        @Override public void cycleFinished(TransitionSystem ts, int cycle) {
            if (capture != null) capture.cycleFinished(ts, cycle);
            CycleTruth c = t.current;
            if (c == null || t.frozen) return;
            snapshot(ts, c);
            t.cycles.put(cycle, c);
            t.lastCycle = cycle;
        }

        @Override public void phaseStarting(TransitionSystem ts, Phase phase) {
            if (capture != null) capture.phaseStarting(ts, phase);
        }

        @Override public void stepStarting(TransitionSystem ts, Step step) {
            if (capture != null) capture.stepStarting(ts, step);
            CycleTruth c = t.current;
            if (c == null) return;
            Circumstance C = ts.getC();
            if (step == Step.ADD_INTENDED_MEANS && C.getSelectedOption() != null) {
                Plan p = C.getSelectedOption().getPlan();
                c.chosenPlans.add(p.getLabel() == null ? String.valueOf(p.getTrigger()) : p.getLabel().getFunctor());
            }
        }

        @Override public void stepFinished(TransitionSystem ts, Step step) {
            if (capture != null) capture.stepFinished(ts, step);
            CycleTruth c = t.current;
            if (c == null) return;
            Circumstance C = ts.getC();
            switch (step) {
                case SELECT_EVENT -> {
                    if (C.getSelectedEvent() != null) c.selectedEvents.add(trigger(C.getSelectedEvent().getTrigger()));
                }
                case SELECT_INTENTION -> {
                    Intention i = C.getSelectedIntention();
                    if (i != null) c.selectedIntentions.add(stack(i));
                }
                case EXECUTE_INTENTION -> {

                    if (C.getLastDeed() != null && !C.getLastDeed().isEmptyBody()) c.executions++;
                }
                default -> { }
            }
        }

        @Override public void planSearchStarting(TransitionSystem ts, Event event, SearchMethod method) {
            if (capture != null) capture.planSearchStarting(ts, event, method);
        }

        @Override public void planEvaluated(TransitionSystem ts, Plan plan, boolean relevant, boolean applicable, List<TermResult> terms) {
            if (capture != null) capture.planEvaluated(ts, plan, relevant, applicable, terms);
        }

        @Override public void messageSent(TransitionSystem ts, Message m) {
            if (capture != null) capture.messageSent(ts, m);
            CycleTruth c = t.current;
            if (c == null) return;
            c.messagesSent++;
            t.sent.add(t.cycle + "|" + m.getSender() + ">" + m.getReceiver() + "|" + m.getIlForce() + "|" + m.getPropCont());
        }

        private void snapshot(TransitionSystem ts, CycleTruth c) {
            Agent ag = ts.getAg();
            for (Literal l : ag.getBB()) c.beliefs.add(belief(l));
            Collections.sort(c.beliefs);
            Circumstance C = ts.getC();
            for (Iterator<Intention> it = C.getAllIntentions(); it.hasNext(); ) {
                Intention i = it.next();
                if (i.isFinished()) continue;
                c.intentions.add(stack(i) + " | " + state(C, i));
            }
            Collections.sort(c.intentions);
            for (Iterator<Event> it = C.getEventsPlusAtomic(); it.hasNext(); ) c.events.add(trigger(it.next().getTrigger()));
            for (Event e : C.getPendingEvents().values()) c.events.add(trigger(e.getTrigger()));
            Collections.sort(c.events);
            c.plans = ag.getPL().getPlans().size();
        }
    }

    static String belief(Literal l) {
        Literal c = l.copy();
        List<String> annots = new ArrayList<>();
        if (l.hasAnnot()) for (Term a : l.getAnnots()) annots.add(a.toString());
        Collections.sort(annots);
        c.clearAnnots();
        return c + " " + annots;
    }

    static String trigger(Trigger t) {
        Literal l = t.getLiteral().copy();
        l.clearAnnots();
        return t.getOperator().toString() + t.getType() + l;
    }

    static String stack(Intention i) {
        List<String> s = new ArrayList<>();
        for (IntendedMeans im : i) s.add(trigger(im.getTrigger()));
        Collections.reverse(s);
        return String.join(" ; ", s);
    }

    static String state(Circumstance C, Intention i) {
        if (i.isSuspended()) return "SUSPENDED";
        if (C.getAction() != null && C.getAction().getIntention() == i) return "WAITING_ACTION";
        for (ActionExec a : C.getFeedbackActions()) if (a.getIntention() == i) return "WAITING_ACTION";
        for (ActionExec a : C.getPendingActions().values()) if (a.getIntention() == i) return "WAITING_ACTION";
        for (Iterator<Event> it = C.getEventsPlusAtomic(); it.hasNext(); ) if (it.next().getIntention() == i) return "WAITING_EVENT";
        for (Event e : C.getPendingEvents().values()) if (e.getIntention() == i) return "WAITING_EVENT";
        if (C.getPendingIntentions().containsValue(i)) return "PENDING";
        if (C.hasRunningIntention(i)) return "QUEUED";
        if (C.getSelectedIntention() == i) return "SELECTED";
        return "NONE";
    }

    public static final class Comparison {
        final String base;
        final javax.json.JsonReaderFactory readers = Json.createReaderFactory(null);
        final Map<String, Object> report = new LinkedHashMap<>();
        final Map<String, int[]> totals = new TreeMap<>();
        final List<Map<String, Object>> divergences = new ArrayList<>();
        long requests;
        long requestNanos;

        final Map<String, AgentTruth> truth;

        public Comparison(String base, Map<String, AgentTruth> truth) {
            this.base = base;
            this.truth = truth;
        }

        public String run() throws Exception {
            for (AgentTruth t : truth.values()) {
                List<Integer> cycles = new ArrayList<>(t.cycles.keySet());
                Collections.sort(cycles);
                for (int n : cycles) compareCycle(t, n, t.cycles.get(n));
                compareMessages(t);
            }
            report.put("agents", truth.size());
            Map<String, Object> checks = new LinkedHashMap<>();
            totals.forEach((k, v) -> checks.put(k, Map.of("ok", v[0], "divergent", v[1])));
            report.put("checks", checks);
            report.put("requests", requests);
            report.put("meanRequestMilliseconds", requests == 0 ? 0 : requestNanos / 1e6 / requests);
            report.put("divergences", divergences.size() > 300 ? divergences.subList(0, 300) : divergences);
            report.put("divergenceCount", divergences.size());
            return Json.createObjectBuilder(toJsonMap(report)).build().toString();
        }

        public String summary() {
            StringBuilder s = new StringBuilder();
            totals.forEach((k, v) -> s.append(k).append('=').append(v[0]).append('/').append(v[0] + v[1]).append(' '));
            return s.toString();
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> toJsonMap(Map<String, Object> m) {
            return m;
        }

        void check(String name, String agent, Integer cycle, Object expected, Object actual) {
            int[] v = totals.computeIfAbsent(name, k -> new int[2]);
            if (expected.equals(actual)) {
                v[0]++;
                return;
            }
            v[1]++;
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("check", name);
            d.put("agent", agent);
            d.put("cycle", cycle == null ? -1 : cycle);
            d.put("jason", String.valueOf(expected));
            d.put("api", String.valueOf(actual));
            divergences.add(d);
        }

        JsonObject get(String path) throws Exception {
            long t0 = System.nanoTime();
            int code;
            String body;
            try {
                java.net.HttpURLConnection con = (java.net.HttpURLConnection) URI.create(base + path).toURL().openConnection(java.net.Proxy.NO_PROXY);
                con.setConnectTimeout(5000);
                con.setReadTimeout(10000);
                code = con.getResponseCode();
                java.io.InputStream in = code >= 400 ? con.getErrorStream() : con.getInputStream();
                body = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("check", "http.timeout");
                d.put("agent", path);
                d.put("cycle", -1);
                d.put("jason", "resposta");
                d.put("api", String.valueOf(e));
                divergences.add(d);
                totals.computeIfAbsent("http.timeout", k -> new int[2])[1]++;
                return null;
            }
            requestNanos += System.nanoTime() - t0;
            requests++;
            if (code != 200) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("check", "http");
                d.put("agent", path);
                d.put("cycle", code);
                d.put("jason", "200");
                d.put("api", code + " " + body);
                divergences.add(d);
                return null;
            }
            try (JsonReader reader = readers.createReader(new StringReader(body))) {
                return reader.readObject();
            }
        }

        void compareCycle(AgentTruth t, int n, CycleTruth truth) throws Exception {
            String a = enc(t.name);
            JsonObject s = get("/api/agents/" + a + "?cycle=" + n);
            if (s == null) return;
            check("cycle.notClamped", t.name, n, n, s.getInt("cycle"));

            List<String> beliefs = new ArrayList<>();
            for (JsonValue v : s.getJsonArray("beliefs")) {
                JsonObject b = v.asJsonObject();
                if ("removed".equals(str(b, "change"))) continue;
                List<String> annots = new ArrayList<>();
                for (JsonValue x : b.getJsonArray("annotations")) annots.add(((javax.json.JsonString) x).getString());
                Collections.sort(annots);
                beliefs.add(b.getString("content") + " " + annots);
            }
            Collections.sort(beliefs);
            check("state.beliefs", t.name, n, truth.beliefs, beliefs);

            Map<String, String> levelTrigger = new LinkedHashMap<>();
            for (JsonValue v : s.getJsonArray("intentions")) {
                JsonObject i = v.asJsonObject();
                for (JsonValue l : i.getJsonArray("levels")) levelTrigger.put(l.asJsonObject().getString("intendedMeansId"), l.asJsonObject().getString("trigger"));
            }
            List<String> intentions = new ArrayList<>();
            for (JsonValue v : s.getJsonArray("intentions")) {
                JsonObject i = v.asJsonObject();
                List<String> st = new ArrayList<>();
                for (JsonValue l : i.getJsonArray("levels")) st.add(l.asJsonObject().getString("trigger"));
                intentions.add(String.join(" ; ", st) + " | " + i.getString("state"));
            }
            Collections.sort(intentions);
            check("state.intentions", t.name, n, truth.intentions, intentions);
            check("state.intentionCount", t.name, n, truth.intentions.size(), intentions.size());

            List<String> events = new ArrayList<>();
            for (JsonValue v : s.getJsonArray("events")) {
                JsonObject e = v.asJsonObject();
                if ("removed".equals(str(e, "change"))) continue;
                events.add(e.getString("trigger"));
            }
            Collections.sort(events);
            check("state.events", t.name, n, truth.events, events);

            JsonObject r = get("/api/agents/" + a + "/cycles/" + n + "/reasoning");
            if (r == null) return;
            List<String> se = new ArrayList<>(), plans = new ArrayList<>(), si = new ArrayList<>();
            int executions = 0;
            for (JsonValue v : r.getJsonArray("steps")) {
                JsonObject step = v.asJsonObject();
                String name = step.getString("step");
                JsonObject chosen = step.isNull("chosen") ? null : step.getJsonObject("chosen");
                switch (name) {
                    case "SELECT_EVENT" -> { if (chosen != null) se.add(chosen.getString("label")); }
                    case "FIND_OPTION" -> { if (chosen != null) plans.add(chosen.getString("label")); }
                    case "SELECT_INTENTION" -> { if (chosen != null) si.add(chosen.getString("id")); }
                    case "EXECUTE_INTENTION" -> {
                        for (JsonValue c : step.getJsonArray("changed"))
                            if ("currentDeed".equals(c.asJsonObject().getString("timeline"))) { executions++; break; }
                    }
                    default -> { }
                }
            }
            check("reasoning.selectedEvent", t.name, n, truth.selectedEvents, se);
            check("reasoning.chosenPlan", t.name, n, truth.chosenPlans, plans);
            check("reasoning.selectedIntentionCount", t.name, n, truth.selectedIntentions.size(), si.size());
            check("reasoning.executions", t.name, n, truth.executions, executions);

            JsonObject c = get("/api/console?agents=" + a + "&until=" + s.getJsonNumber("sequenceEnd").longValue() + "&limit=100000");
            if (c != null) {
                int lines = 0;
                for (JsonValue v : c.getJsonArray("lines")) if (v.asJsonObject().getInt("cycle") == n) lines++;
                check("console.linesPerCycle", t.name, n, truth.consoleLines, lines);
            }
        }

        void compareMessages(AgentTruth t) throws Exception {
            if (t.lastCycle < 0) return;
            JsonObject s = get("/api/agents/" + enc(t.name) + "?cycle=" + t.lastCycle + "&include=events");
            if (s == null) return;
            long until = s.getJsonNumber("sequenceEnd").longValue();
            JsonObject m = get("/api/messages?senders=" + enc(t.name) + "&until=" + until + "&limit=100000");
            if (m == null) return;
            List<String> api = new ArrayList<>();
            for (JsonValue v : m.getJsonArray("messages")) {
                JsonObject o = v.asJsonObject();
                JsonObject sent = o.isNull("sent") ? null : o.getJsonObject("sent");
                if (sent == null || sent.getInt("cycle") > t.lastCycle) continue;
                api.add(sent.getInt("cycle") + "|" + o.getString("sender") + ">" + o.getString("receiver") + "|" + o.getString("performative") + "|" + o.getString("content"));
            }
            List<String> truth = new ArrayList<>();
            synchronized (t.sent) {
                for (String x : t.sent) if (Integer.parseInt(x.substring(0, x.indexOf('|'))) <= t.lastCycle) truth.add(x);
            }
            Collections.sort(api);
            Collections.sort(truth);
            check("messages.sent", t.name, t.lastCycle, truth, api);
        }

        static String str(JsonObject o, String k) {
            return o.containsKey(k) && !o.isNull(k) ? o.getString(k) : null;
        }

        static String enc(String s) {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        }
    }
}
