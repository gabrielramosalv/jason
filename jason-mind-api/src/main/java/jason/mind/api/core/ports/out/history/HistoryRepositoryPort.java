package jason.mind.api.core.ports.out.history;

import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.trace.PlanSelection;

import java.util.Optional;

/**
 * Port for writing the history of the agents.
 *
 * @author Gabriel Lima
 */
public interface HistoryRepositoryPort {

    /**
     * Loads an agent by name.
     *
     * @param name name of the agent
     * @return agent, or empty when unknown
     */
    Optional<Agent> loadAgent(String name);

    /**
     * Saves an agent.
     *
     * @param agent agent to save
     */
    void saveAgent(Agent agent);

    /**
     * Finds an entity of an agent by identifier.
     *
     * @param agentName name of the agent
     * @param id        identifier of the entity
     * @return entity, or empty when unknown
     */
    Optional<Entity> findEntity(String agentName, long id);

    /**
     * Appends an entity to the history of an agent.
     *
     * @param agentName name of the agent
     * @param entity    entity to append
     */
    void appendEntity(String agentName, Entity entity);

    /**
     * Registers a message for its receiver.
     *
     * @param receiverName name of the receiving agent
     * @param message      message to register
     */
    void registerMessage(String receiverName, Message message);

    /**
     * Appends an occurrence to the history of an agent.
     *
     * @param agentName name of the agent
     * @param record    occurrence to append
     */
    void appendOccurrence(String agentName, OccurrenceRecord record);

    /**
     * Appends a plan selection to the history of an agent.
     *
     * @param agentName name of the agent
     * @param selection plan selection to append
     */
    void appendPlanSelection(String agentName, PlanSelection selection);

    /**
     * Commits a finished cycle of an agent.
     *
     * @param agentName name of the agent
     * @param cycle     finished cycle
     */
    void commitCycle(String agentName, CycleRecord cycle);

    /**
     * Prunes the history of an agent before a cycle.
     *
     * @param agentName   name of the agent
     * @param oldestCycle oldest cycle kept
     */
    void prune(String agentName, int oldestCycle);
}
