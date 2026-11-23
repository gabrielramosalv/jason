package jason.mind.api.core.ports.out.history;

import java.util.Optional;

import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.trace.PlanSelection;

public interface HistoryRepositoryPort {

    Optional<Agent> loadAgent(String name);

    void saveAgent(Agent agent);

    Optional<Entity<?>> findEntity(String agentName, long id);

    void appendEntity(String agentName, Entity<?> entity);

    void registerMessage(String receiverName, Message message);

    void appendOccurrence(String agentName, OccurrenceRecord record);

    void appendPlanSelection(String agentName, PlanSelection selection);

    void commitCycle(String agentName, CycleRecord cycle);

    void prune(String agentName, int oldestCycle);
}
