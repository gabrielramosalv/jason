package jason.mind.api.core.application.metrics;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.service.history.HistoryFootprintEstimator;
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
            long bytes = HistoryFootprintEstimator.bytes(f);
            OptionalDouble mean = this.captureTime.meanNanos(name);
            agents.add(new MetricsOut.AgentMetrics(name, retained, bytes, mean.isPresent() ? mean.getAsDouble() / 1000.0 : null,
                    f.entities(), f.timelineEntries(), f.effects()));
        }
        agents.sort(Comparator.comparing(MetricsOut.AgentMetrics::name));
        long max = this.memory.heapMaxBytes();
        return new MetricsOut(this.memory.heapUsedBytes(), max < 0 ? null : max, agents);
    }
}
