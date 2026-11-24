package jason.mind.api.infrastructure.metrics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Benchmark timing of the Mind API, switched on by editing {@link #TIMING}.
 * Accumulates, per agent, the time spent inside the capture and the part of it spent recording into the history,
 * and, per route, the time spent in the use case and in the JSON serialization. With {@link #TIMING} off, every
 * method returns at once and the compiler drops the timing code.
 *
 * @author Gabriel Lima
 */
public final class MindTiming {

    /** Switch for benchmark timing: set to true to accumulate the times. */
    public static final boolean TIMING = false;

    /** Time spent inside the capture, by agent. */
    private static final Map<String, Stat> CAPTURE = new ConcurrentHashMap<>();

    /** Time spent recording into the history, by agent; part of the capture time. */
    private static final Map<String, Stat> RECORDING = new ConcurrentHashMap<>();

    /** Time spent in the use case of each route, including the ETag. */
    private static final Map<String, Stat> USE_CASE = new ConcurrentHashMap<>();

    /** Time spent serializing the response of each route to JSON. */
    private static final Map<String, Stat> SERIALIZATION = new ConcurrentHashMap<>();

    /** Time between the server handing the request to the framework (T0) and the exchange closed (T4), by route. */
    private static final Map<String, Stat> REQUEST = new ConcurrentHashMap<>();

    /** Route handled by the current thread, so the serialization can be attributed to it. */
    private static final ThreadLocal<String> ROUTE = new ThreadLocal<>();

    /**
     * Prevents instantiation of this utility class.
     */
    private MindTiming() {
    }

    /**
     * Adds a measured duration to the statistics of a key.
     *
     * @param stats       statistics by key
     * @param key         agent or route the duration belongs to
     * @param nanoseconds measured duration
     */
    private static void add(Map<String, Stat> stats, String key, long nanoseconds) {
        stats.computeIfAbsent(key == null ? "?" : key, k -> new Stat()).add(nanoseconds);
    }

    /**
     * Adds time spent inside the capture of an agent.
     *
     * @param agent       name of the agent
     * @param nanoseconds measured duration
     */
    public static void addCapture(String agent, long nanoseconds) {
        add(CAPTURE, agent, nanoseconds);
    }

    /**
     * Adds time spent recording into the history of an agent.
     *
     * @param agent       name of the agent
     * @param nanoseconds measured duration
     */
    public static void addRecording(String agent, long nanoseconds) {
        add(RECORDING, agent, nanoseconds);
    }

    /**
     * Runs the use case of a route, timing it when {@link #TIMING} is on and remembering the route for the
     * serialization that follows on the same thread.
     *
     * @param route name of the route
     * @param body  use case to run
     * @param <T>   type of the result
     * @return result of the use case
     */
    public static <T> T timeRoute(String route, Supplier<T> body) {
        if (!TIMING) {
            return body.get();
        }
        ROUTE.set(route);
        long t0 = System.nanoTime();
        try {
            return body.get();
        } finally {
            add(USE_CASE, route, System.nanoTime() - t0);
        }
    }

    /**
     * Adds time spent serializing a response to the route last handled by the current thread.
     *
     * @param nanoseconds measured duration
     */
    public static void addSerialization(long nanoseconds) {
        add(SERIALIZATION, ROUTE.get(), nanoseconds);
    }

    /**
     * Adds the whole time of a request, from the server handing it to the framework to the exchange closed, to the
     * route last handled by the current thread, then forgets that route so it cannot leak into a request that does
     * not reach a route (unknown path, for example). Meant to be called from the thread that handled the request.
     *
     * @param nanoseconds measured duration
     */
    public static void addRequest(long nanoseconds) {
        if (!TIMING) {
            return;
        }
        add(REQUEST, ROUTE.get(), nanoseconds);
        ROUTE.remove();
    }

    /**
     * Copies the accumulated statistics.
     *
     * @return statistics by group ({@code capture}, {@code recording}, {@code useCase}, {@code serialization},
     *         {@code request}), then by agent or route
     */
    public static Map<String, Map<String, StatOut>> snapshot() {
        Map<String, Map<String, StatOut>> r = new LinkedHashMap<>();
        r.put("capture", copy(CAPTURE));
        r.put("recording", copy(RECORDING));
        r.put("useCase", copy(USE_CASE));
        r.put("serialization", copy(SERIALIZATION));
        r.put("request", copy(REQUEST));
        return r;
    }

    /**
     * Copies the statistics of one group, ordered by key.
     *
     * @param stats statistics by key
     * @return copied statistics by key
     */
    private static Map<String, StatOut> copy(Map<String, Stat> stats) {
        Map<String, StatOut> r = new TreeMap<>();
        stats.forEach((k, s) -> r.put(k, s.out()));
        return r;
    }

    /**
     * Count, total and maximum of the durations measured for one key.
     *
     * @param count            number of measured durations
     * @param totalNanoseconds sum of the durations
     * @param maxNanoseconds   longest duration
     */
    public record StatOut(long count, long totalNanoseconds, long maxNanoseconds) {
    }

    /**
     * Thread-safe accumulator of durations.
     */
    private static final class Stat {

        /** Number of measured durations. */
        private final LongAdder count = new LongAdder();

        /** Sum of the durations. */
        private final LongAdder total = new LongAdder();

        /** Longest duration. */
        private final LongAccumulator max = new LongAccumulator(Math::max, 0);

        /**
         * Adds one measured duration.
         *
         * @param nanoseconds measured duration
         */
        void add(long nanoseconds) {
            this.count.increment();
            this.total.add(nanoseconds);
            this.max.accumulate(nanoseconds);
        }

        /**
         * Copies the accumulated values.
         *
         * @return count, total and maximum
         */
        StatOut out() {
            return new StatOut(this.count.sum(), this.total.sum(), this.max.get());
        }
    }
}
