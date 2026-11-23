package jason.mind.api.infrastructure.adapters.out.persistence.memory.history;

import java.util.Optional;

import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;

public class HistoryRepositoryAdapter implements HistoryRepositoryPort {

    private final HistoryMemory memory;

    public HistoryRepositoryAdapter(HistoryMemory memory) {
        this.memory = memory;
    }

    @Override
    public Optional<Agent> loadAgent(String name) {
        AgentStore s = memory.store(name);
        return s == null ? Optional.empty() : Optional.of(s.agent);
    }

    @Override
    public void saveAgent(Agent agent) {
        AgentStore s = memory.store(agent.getName());
        if (s != null && s.agent == agent) return;
        memory.stores.put(agent.getName(), new AgentStore(agent));
    }

    @Override
    public Optional<Entity<?>> findEntity(String agentName, long id) {
        AgentStore s = memory.store(agentName);
        if (s == null) return Optional.empty();
        Entity<?> e = s.entities.get(id);
        if (e == null) e = s.incoming.get(id);
        return Optional.ofNullable(e);
    }

    @Override
    public void appendEntity(String agentName, Entity<?> entity) {
        AgentStore s = require(agentName);
        if (entity instanceof Message m && m.getStart() == null) s.incoming.put(m.getId(), m);
        else s.entities.put(entity.getId(), entity);
    }

    @Override
    public void registerMessage(String receiverName, Message message) {
        AgentStore s = memory.store(receiverName);
        if (s != null) s.incoming.put(message.getId(), message);
    }

    @Override
    public void appendOccurrence(String agentName, OccurrenceRecord record) {
        require(agentName).append(record);
    }

    @Override
    public void appendPlanSelection(String agentName, PlanSelection selection) {
        require(agentName).append(selection);
    }

    @Override
    public void commitCycle(String agentName, CycleRecord cycle) {
        AgentStore s = require(agentName);
        s.cycles.put(cycle.cycle(), cycle);
        s.lastClosed = cycle;
    }

    @Override
    public void prune(String agentName, int oldestCycle) {
        require(agentName).prune(oldestCycle);
    }

    private AgentStore require(String agentName) {
        AgentStore s = memory.store(agentName);
        if (s == null) throw new IllegalStateException("agent without store: " + agentName);
        return s;
    }
}
