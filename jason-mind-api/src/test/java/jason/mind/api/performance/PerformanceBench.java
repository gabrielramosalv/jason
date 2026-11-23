package jason.mind.api.performance;

import jason.asSemantics.Agent;
import jason.asSemantics.Event;
import jason.asSemantics.Message;
import jason.asSemantics.ReasoningListener;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Plan;
import jason.infra.local.BaseLocalMAS;
import jason.infra.local.LocalAgArch;
import jason.infra.local.RunLocalMAS;
import jason.mind.api.bootstrap.MindApiManager;
import jason.util.Config;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class PerformanceBench {

    static final AtomicLong CAPTURE_NANOS = new AtomicLong();
    static final AtomicLong CAPTURE_CALLS = new AtomicLong();

    public static void main(String[] args) throws Exception {
        String mas = args[0];
        String mode = args[1];
        boolean on = mode.equals("on");
        int warmup = Integer.parseInt(args[2]);
        int seconds = Integer.parseInt(args[3]);
        Path report = Path.of(args[4]);
        int heapSamples = args.length > 5 ? Integer.parseInt(args[5]) : 5;
        List<String> runArgs = new ArrayList<>(List.of(mas, "--no-rmi", "--no-mbean", "--log-conf",
                System.getProperty("bench.logConf", "")));
        if (on) Config.get().setProperty(Config.MIND_INSP_WEB_SERVER_CLASS_NAME, TimedManager.class.getName());
        else if (mode.equals("old")) {

            Config.get().setMindInspectorArchClassName(OldWithHistory.class.getName());
            Config.get().setProperty(Config.MIND_INSP_WEB_SERVER_CLASS_NAME, jason.architecture.MindInspectorWebImpl.class.getName());
        } else runArgs.add("--no-mindinspector");

        Thread runner = new Thread(() -> {
            try {
                RunLocalMAS.main(runArgs.toArray(new String[0]));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "bench-mas");
        runner.setDaemon(true);
        runner.start();
        Thread.sleep(warmup * 1000L);

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Map<String, LocalAgArch> ags = BaseLocalMAS.getRunner().getAgs();
        Map<String, Long> c0 = cycles(ags);
        long count0 = counts(ags);
        long cpu0 = os.getProcessCpuTime();
        long agentCpu0 = agentThreadsCpu(threads);
        long cap0 = CAPTURE_NANOS.get(), calls0 = CAPTURE_CALLS.get();
        long t0 = System.nanoTime();
        List<Long> heap = new ArrayList<>();
        List<Long> cyclesAtSample = new ArrayList<>();
        String json = "";
        for (int k = 0; k < heapSamples; k++) {
            Thread.sleep(seconds * 1000L / heapSamples);
            System.gc();
            heap.add(memory.getHeapMemoryUsage().getUsed());
            Map<String, Long> now = cycles(ags);
            long total = 0;
            for (long c : now.values()) total += c;
            cyclesAtSample.add(total / Math.max(1, now.size()));

            json = report(mas, mode, c0, now, count0, counts(ags), os.getProcessCpuTime() - cpu0,
                    agentThreadsCpu(threads) - agentCpu0, CAPTURE_NANOS.get() - cap0, CAPTURE_CALLS.get() - calls0,
                    System.nanoTime() - t0, heap, cyclesAtSample, k == heapSamples - 1);
            Files.writeString(report, json);
        }
        System.out.print(json);
        Runtime.getRuntime().halt(0);
    }

    static Map<String, Long> cycles(Map<String, LocalAgArch> ags) {
        Map<String, Long> r = new java.util.TreeMap<>();
        for (LocalAgArch a : new ArrayList<>(ags.values())) r.put(a.getAgName(), (long) a.getCycleNumber());
        return r;
    }

    static String report(String mas, String mode, Map<String, Long> c0, Map<String, Long> c1, long count0, long count1,
                         long cpu, long agentCpu, long cap, long calls, long elapsed, List<Long> heap,
                         List<Long> cyclesAtSample, boolean complete) {
        long totalCycles = 0;
        int agents = 0;
        for (Map.Entry<String, Long> e : c1.entrySet()) {
            Long before = c0.get(e.getKey());
            totalCycles += e.getValue() - (before == null ? 0 : before);
            agents++;
        }
        double secs = elapsed / 1e9;
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append(String.format(Locale.ROOT, "  \"mas\": \"%s\",\n  \"mode\": \"%s\",\n  \"complete\": %s,\n  \"agents\": %d,\n  \"seconds\": %.1f,\n",
                mas, mode, complete, agents, secs));
        json.append(String.format(Locale.ROOT, "  \"cyclesPerSecondPerAgent\": %.1f,\n", totalCycles / secs / Math.max(1, agents)));
        json.append(String.format(Locale.ROOT, "  \"processCpuCores\": %.2f,\n", cpu / 1e9 / secs));
        json.append(String.format(Locale.ROOT, "  \"agentThreadsCpuCores\": %.2f,\n", agentCpu / 1e9 / secs));
        json.append(String.format(Locale.ROOT, "  \"processCpuMicrosecondsPerCycle\": %.2f,\n", totalCycles == 0 ? 0 : cpu / 1e3 / totalCycles));
        json.append(String.format(Locale.ROOT, "  \"captureMicrosecondsPerCycle\": %.2f,\n", totalCycles == 0 ? 0 : cap / 1e3 / totalCycles));
        json.append(String.format(Locale.ROOT, "  \"captureCallsPerCycle\": %.1f,\n", totalCycles == 0 ? 0 : (double) calls / totalCycles));

        json.append(String.format(Locale.ROOT, "  \"loopIterationsPerSecondPerAgent\": %.1f,\n", (count1 - count0) / secs / Math.max(1, agents)));
        json.append("  \"heapAfterGcMegabytes\": [");
        for (int k = 0; k < heap.size(); k++) json.append(k == 0 ? "" : ", ").append(String.format(Locale.ROOT, "%.1f", heap.get(k) / 1048576.0));
        json.append("],\n  \"cyclesPerAgentAtHeapSample\": ").append(cyclesAtSample).append("\n}\n");
        return json.toString();
    }

    static long counts(Map<String, LocalAgArch> ags) {
        long sum = 0;
        for (LocalAgArch a : ags.values()) {
            for (jason.asSyntax.Literal l : a.getTS().getAg().getBB()) {
                if (l.getFunctor().equals("count") && l.getArity() == 1 && l.getTerm(0).isNumeric()) {
                    try { sum += (long) ((jason.asSyntax.NumberTerm) l.getTerm(0)).solve(); } catch (Exception e) { }
                }
            }
        }
        return sum;
    }

    static long agentThreadsCpu(ThreadMXBean threads) {
        long sum = 0;
        for (var info : threads.dumpAllThreads(false, false)) {
            String n = info.getThreadName();
            if (n.startsWith("bench-") || n.startsWith("HTTP-") || n.startsWith("Reference") || n.startsWith("Finalizer")
                    || n.startsWith("Signal") || n.startsWith("Common-Cleaner") || n.equals("main") || n.startsWith("Attach")
                    || n.startsWith("Notification")) continue;
            long t = threads.getThreadCpuTime(info.getThreadId());
            if (t > 0) sum += t;
        }
        return sum;
    }

    public static final class TimedManager extends MindApiManager {
        @Override
        public void registerAg(Agent agent) {
            super.registerAg(agent);
            ReasoningListener capture = agent.getTS().getReasoningListener();
            if (capture != null) agent.getTS().setReasoningListener(new Timed(capture));
        }
    }

    static final class Timed implements ReasoningListener {
        private final ReasoningListener d;

        Timed(ReasoningListener d) {
            this.d = d;
        }

        private static void add(long start) {
            CAPTURE_NANOS.addAndGet(System.nanoTime() - start);
            CAPTURE_CALLS.incrementAndGet();
        }

        @Override public void cycleStarting(TransitionSystem ts, int cycle) { long s = System.nanoTime(); d.cycleStarting(ts, cycle); add(s); }
        @Override public void cycleFinished(TransitionSystem ts, int cycle) { long s = System.nanoTime(); d.cycleFinished(ts, cycle); add(s); }
        @Override public void phaseStarting(TransitionSystem ts, Phase phase) { long s = System.nanoTime(); d.phaseStarting(ts, phase); add(s); }
        @Override public void stepStarting(TransitionSystem ts, Step step) { long s = System.nanoTime(); d.stepStarting(ts, step); add(s); }
        @Override public void stepFinished(TransitionSystem ts, Step step) { long s = System.nanoTime(); d.stepFinished(ts, step); add(s); }
        @Override public void planSearchStarting(TransitionSystem ts, Event event, SearchMethod method) { long s = System.nanoTime(); d.planSearchStarting(ts, event, method); add(s); }
        @Override public void planEvaluated(TransitionSystem ts, Plan plan, boolean relevant, boolean applicable, List<TermResult> terms) { long s = System.nanoTime(); d.planEvaluated(ts, plan, relevant, applicable, terms); add(s); }
        @Override public void messageSent(TransitionSystem ts, Message m) { long s = System.nanoTime(); d.messageSent(ts, m); add(s); }
    }

    public static final class OldWithHistory extends jason.architecture.MindInspectorAgArch {
        @Override
        public void init() {
            setupMindInspector("web(cycle,html,history)");
        }
    }
}
