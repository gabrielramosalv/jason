package jason.mind.api.core.application.mas;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.ports.in.mas.FindMasPort;
import jason.mind.api.core.ports.in.mas.MasOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.mas.MasRepositoryPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Use case that describes the multi-agent system.
 * It returns the system name, the versions in use and every agent, sorted by name, with its
 * activity, end flag and retained cycle window.
 *
 * @author Gabriel Lima
 */
public class FindMasUseCase implements FindMasPort {

    /** Read-only access to the agents and their cycles. */
    private final HistoryQueryRepositoryPort historyQuery;

    /** Repository that holds the system name. */
    private final MasRepositoryPort masRepository;

    /** Versions of the Mind API and of Jason reported with the system. */
    private final MasOut.Versions versions;

    /**
     * Creates the use case with the history, the system repository and the versions to report.
     *
     * @param historyQuery  {@link #historyQuery}
     * @param masRepository {@link #masRepository}
     * @param mindVersion   version of the Mind API
     * @param jasonVersion  version of Jason
     */
    public FindMasUseCase(HistoryQueryRepositoryPort historyQuery, MasRepositoryPort masRepository, String mindVersion,
                          String jasonVersion) {
        this.historyQuery = historyQuery;
        this.masRepository = masRepository;
        this.versions = new MasOut.Versions(mindVersion, jasonVersion);
    }

    @Override
    public MasOut execute() {
        List<MasOut.AgentItem> agents = new ArrayList<>();
        for (Agent a : this.historyQuery.agents()) {
            Optional<CycleRecord> last = this.historyQuery.lastClosedCycle(a.getName());
            Optional<CycleRecord> oldest = this.historyQuery.oldestCycle(a.getName());
            boolean active = last.map(c -> c.sense() || c.deliberate() || c.act()).orElse(false);
            agents.add(
                    new MasOut.AgentItem(a.getName(), active, a.hasEnded(), oldest.map(CycleRecord::cycle).orElse(null),
                            last.map(CycleRecord::cycle).orElse(null)));
        }
        agents.sort(Comparator.comparing(MasOut.AgentItem::name));
        return new MasOut(this.masRepository.name(), this.versions, agents);
    }
}
