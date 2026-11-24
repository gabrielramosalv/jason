package jason.mind.api.infrastructure.adapters.out.metrics;

import jason.mind.api.core.ports.out.metrics.CaptureTimeRepositoryPort;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the capture time of the latest cycles of each agent in a ring, in memory.
 *
 * @author Gabriel Lima
 */
public class CaptureTimeRepositoryAdapter implements CaptureTimeRepositoryPort {

    /** Number of cycles the mean covers. */
    static final int WINDOW = 100;

    /** One ring per agent. */
    private final Map<String, Ring> rings = new ConcurrentHashMap<>();

    @Override
    public void record(String agent, long nanos) {
        this.rings.computeIfAbsent(agent, k -> new Ring()).add(nanos);
    }

    @Override
    public OptionalDouble meanNanos(String agent) {
        Ring r = this.rings.get(agent);
        return r == null ? OptionalDouble.empty() : r.mean();
    }

    /** Latest values of one agent; the thread of the agent writes once per cycle and the HTTP threads read. */
    private static final class Ring {

        /** Values in a circular buffer. */
        private final long[] values = new long[WINDOW];

        /** Number of values recorded so far, up to the window. */
        private int count;

        /** Position of the next write. */
        private int next;

        /** Sum of the values held. */
        private long sum;

        synchronized void add(long nanos) {
            if (this.count == WINDOW) {
                this.sum -= this.values[this.next];
            } else {
                this.count++;
            }
            this.values[this.next] = nanos;
            this.sum += nanos;
            this.next = (this.next + 1) % WINDOW;
        }

        synchronized OptionalDouble mean() {
            return this.count == 0 ? OptionalDouble.empty() : OptionalDouble.of((double) this.sum / this.count);
        }
    }
}
