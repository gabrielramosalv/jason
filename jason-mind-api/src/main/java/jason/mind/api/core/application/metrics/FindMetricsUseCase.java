package jason.mind.api.core.application.metrics;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.ports.in.metrics.FindMetricsPort;
import jason.mind.api.core.ports.in.metrics.MetricsOut;
import jason.mind.api.core.ports.out.history.Footprint;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.metrics.CaptureTimeRepositoryPort;
import jason.mind.api.core.ports.out.metrics.RuntimeMemoryPort;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Use case that reports the light monitoring of the Mind: the heap of the JVM and, per agent, the estimated size of
 * the retained history and the mean capture time.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class FindMetricsUseCase implements FindMetricsPort {

    /**
     * Estimated bytes of one entity held. Fitted by least squares to the heap after GC at 1 000 to 30 000 retained
     * cycles of two examples (a small mind and a mind with 1 000 beliefs), keeping non-negative coefficients; the
     * fit leaves a mean error of 3.6% and a maximum of 14.7% on the history, and it is an estimate, not a measure.
     */
    static final long BYTES_PER_ENTITY = 650;

    /** Estimated bytes of one timeline entry held, from the same fit as {@link #BYTES_PER_ENTITY}. */
    static final long BYTES_PER_ENTRY = 100;

    /**
     * Estimated bytes of one caused occurrence held. Zero: the count grows in proportion to the entries, so the fit
     * cannot tell its cost apart and the entities and entries already carry it.
     */
    static final long BYTES_PER_EFFECT = 0;

    /** Read-only access to the history. */
    private final HistoryQueryRepositoryPort historyQuery;

    /** Capture time of the latest cycles. */
    private final CaptureTimeRepositoryPort captureTime;

    /** Memory of the JVM. */
    private final RuntimeMemoryPort memory;

    @Override
    public MetricsOut execute() {
        List<MetricsOut.AgentMetrics> agents = new ArrayList<>();
        for (Agent a : this.historyQuery.agents()) {
            String name = a.getName();
            Optional<CycleRecord> oldest = this.historyQuery.oldestCycle(name);
            Optional<CycleRecord> newest = this.historyQuery.lastClosedCycle(name);
            int retained = oldest.isPresent() && newest.isPresent() ? newest.get().cycle() - oldest.get().cycle() + 1 : 0;
            Footprint f = this.historyQuery.footprint(name);
            long bytes = f.entities() * BYTES_PER_ENTITY + f.timelineEntries() * BYTES_PER_ENTRY
                    + f.effects() * BYTES_PER_EFFECT;
            OptionalDouble mean = this.captureTime.meanNanos(name);
            agents.add(new MetricsOut.AgentMetrics(name, retained, bytes, mean.isPresent() ? mean.getAsDouble() / 1000.0 : null,
                    f.entities(), f.timelineEntries(), f.effects()));
        }
        agents.sort(Comparator.comparing(MetricsOut.AgentMetrics::name));
        long max = this.memory.heapMaxBytes();
        return new MetricsOut(this.memory.heapUsedBytes(), max < 0 ? null : max, agents);
    }
}
