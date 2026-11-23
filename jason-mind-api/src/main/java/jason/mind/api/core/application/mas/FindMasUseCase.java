package jason.mind.api.core.application.mas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.ports.in.mas.FindMasPort;
import jason.mind.api.core.ports.in.mas.MasOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.mas.MasRepositoryPort;

public class FindMasUseCase implements FindMasPort {

    private final HistoryQueryRepositoryPort historyQuery;
    private final MasRepositoryPort masRepository;
    private final MasOut.Versions versions;

    public FindMasUseCase(HistoryQueryRepositoryPort historyQuery, MasRepositoryPort masRepository, String mindVersion, String jasonVersion) {
        this.historyQuery = historyQuery;
        this.masRepository = masRepository;
        this.versions = new MasOut.Versions(mindVersion, jasonVersion);
    }

    @Override
    public MasOut execute() {
        List<MasOut.AgentItem> agents = new ArrayList<>();
        for (Agent a : historyQuery.agents()) {
            Optional<CycleRecord> last = historyQuery.lastClosedCycle(a.getName());
            Optional<CycleRecord> oldest = historyQuery.oldestCycle(a.getName());
            boolean active = last.map(c -> c.sense() || c.deliberate() || c.act()).orElse(false);
            agents.add(new MasOut.AgentItem(a.getName(), active, a.hasEnded(),
                    oldest.map(CycleRecord::cycle).orElse(null), last.map(CycleRecord::cycle).orElse(null)));
        }
        agents.sort((x, y) -> x.name().compareTo(y.name()));
        return new MasOut(masRepository.name(), versions, agents);
    }
}
