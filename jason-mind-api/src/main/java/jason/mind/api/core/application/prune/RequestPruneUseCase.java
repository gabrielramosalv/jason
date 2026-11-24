package jason.mind.api.core.application.prune;

import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.ports.in.prune.PruneOut;
import jason.mind.api.core.ports.in.prune.RequestPrunePort;
import jason.mind.api.core.ports.in.prune.RequestPruneIn;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Use case that registers a manual prune of the history up to a cycle.
 * It does not prune: the history has a single writer, the thread of the agent, which applies the request at the end
 * of its next cycle, at the same point as the prune by retention. The horizon only moves forward, so a request before
 * the current horizon does nothing.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class RequestPruneUseCase implements RequestPrunePort {

    /** History that receives the request. */
    private final HistoryRepositoryPort history;

    /** Read-only access to the agents and their newest cycle. */
    private final HistoryQueryRepositoryPort historyQuery;

    @Override
    public PruneOut execute(RequestPruneIn in) {
        if (in.until() == null || in.until() < 0) {
            throw new InvalidQueryException("until must be a cycle number, zero or more");
        }
        List<Agent> agents = new ArrayList<>();
        if (in.agent() == null) {
            agents.addAll(this.historyQuery.agents());
        } else {
            agents.add(HistoryView.agent(this.historyQuery, in.agent()));
        }
        List<PruneOut.Request> requests = new ArrayList<>();
        for (Agent a : agents) {
            Optional<CycleRecord> newest = this.historyQuery.lastClosedCycle(a.getName());
            if (newest.isEmpty()) {
                continue;
            }
            int until = Math.min(in.until(), newest.get().cycle());
            this.history.requestPrune(a.getName(), until);
            requests.add(new PruneOut.Request(a.getName(), until));
        }
        return new PruneOut(requests);
    }
}
