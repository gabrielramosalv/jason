package jason.mind.api.infrastructure.adapters.out.persistence.memory.history;

import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Adapter that writes the history of the agents to the in-memory history.
 * Creates one store per agent and appends entities and closed cycles to it. Occurrences and plan selections are kept
 * by the entities themselves; only the occurrences caused by an entity and the plan selections of an event are linked
 * here, to the causing entity and to the event.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class HistoryRepositoryAdapter implements HistoryRepositoryPort {

    /** In-memory history of all agents. */
    private final HistoryMemory memory;

    /**
     * Finds the store of an agent that must already exist.
     *
     * @param agentName name of the agent
     * @return store of the agent
     * @throws IllegalStateException when the agent has no store
     */
    private AgentStore requireStore(String agentName) {
        AgentStore s = this.memory.findStore(agentName);
        if (s == null) {
            throw new IllegalStateException("agent without store: " + agentName);
        }
        return s;
    }

    @Override
    public Optional<Agent> loadAgent(String name) {
        AgentStore s = this.memory.findStore(name);
        return s == null ? Optional.empty() : Optional.of(s.agent);
    }

    @Override
    public void saveAgent(Agent agent) {
        AgentStore s = this.memory.findStore(agent.getName());
        if (s != null && s.agent == agent) {
            return;
        }
        this.memory.stores.put(agent.getName(), this.memory.newStore(agent));
    }

    @Override
    public Optional<Entity> findEntity(String agentName, long id) {
        AgentStore s = this.memory.findStore(agentName);
        if (s == null) {
            return Optional.empty();
        }
        Entity e = s.entities.get(id);
        if (e == null) {
            e = s.incoming.get(id);
        }
        return Optional.ofNullable(e);
    }

    @Override
    public void appendEntity(String agentName, Entity entity) {
        AgentStore s = requireStore(agentName);
        if (entity instanceof Message m && m.getStart() == null) {
            s.incoming.put(m.getId(), m);
        } else {
            s.entities.put(entity.getId(), entity);
        }
    }

    @Override
    public void registerMessage(String receiverName, Message message) {
        AgentStore s = this.memory.findStore(receiverName);
        if (s != null) {
            s.incoming.put(message.getId(), message);
        }
    }

    @Override
    public void appendOccurrence(String agentName, OccurrenceRecord record) {
        requireStore(agentName);
    }

    @Override
    public void appendPlanSelection(String agentName, PlanSelection selection) {
        requireStore(agentName);
        selection.event().addSelection(selection);
    }

    @Override
    public void commitCycle(String agentName, CycleRecord cycle) {
        AgentStore s = requireStore(agentName);
        s.cycles.put(cycle.cycle(), cycle);
        s.lastClosed = cycle;
    }

    @Override
    public void requestPrune(String agentName, int oldestCycle) {
        AgentStore s = requireStore(agentName);
        s.pendingPrune.accumulateAndGet(oldestCycle, Math::max);
    }

    @Override
    public int takePrune(String agentName) {
        AgentStore s = this.memory.findStore(agentName);
        return s == null ? 0 : s.pendingPrune.getAndSet(0);
    }

    @Override
    public int horizonForBudget(String agentName, long budgetBytes) {
        AgentStore s = this.memory.findStore(agentName);
        if (s == null || budgetBytes == Long.MAX_VALUE) {
            return 0;
        }
        return s.horizonForBudget(budgetBytes / Math.max(1, this.memory.stores.size()));
    }

    @Override
    public void prune(String agentName, int oldestCycle) {
        AgentStore s = requireStore(agentName);
        s.prune(oldestCycle);
        // An agent that ended and has nothing left inside the retention window has nothing to show: free its store.
        if (s.agent.hasEnded() && s.isEmpty()) {
            this.memory.stores.remove(agentName, s);
        }
    }
}
