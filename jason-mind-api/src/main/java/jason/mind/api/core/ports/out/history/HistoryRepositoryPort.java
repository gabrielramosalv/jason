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

    /**
     * Asks that the history of an agent be pruned before a cycle at the end of its next cycle. Asking again keeps
     * the largest request.
     *
     * @param agentName   name of the agent
     * @param oldestCycle oldest cycle to keep
     */
    void requestPrune(String agentName, int oldestCycle);

    /**
     * Takes the pending prune request of an agent and clears it.
     *
     * @param agentName name of the agent
     * @return oldest cycle to keep, or {@code 0} when nothing was requested
     */
    int takePrune(String agentName);

    /**
     * Finds the oldest cycle to keep so that the history of an agent stays within its share of a memory budget. The
     * budget is shared equally by the agents that have a history.
     *
     * @param agentName   name of the agent
     * @param budgetBytes bytes the history of all agents together may use
     * @return the oldest cycle to keep, or {@code 0} when the history fits
     */
    int horizonForBudget(String agentName, long budgetBytes);
}
