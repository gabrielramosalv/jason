package jason.mind.api.core.ports.out.history;

import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.trace.PlanSelection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Port for reading the recorded history of the agents.
 *
 * @author Gabriel Lima
 */
public interface HistoryQueryRepositoryPort {

    /**
     * Returns all recorded agents.
     *
     * @return recorded agents
     */
    List<Agent> agents();

    /**
     * Finds an agent by name.
     *
     * @param name name of the agent
     * @return agent, or empty when unknown
     */
    Optional<Agent> agent(String name);

    /**
     * Finds the last closed cycle of an agent.
     *
     * @param agent name of the agent
     * @return last closed cycle, or empty when there is none
     */
    Optional<CycleRecord> lastClosedCycle(String agent);

    /**
     * Finds the oldest retained cycle of an agent.
     *
     * @param agent name of the agent
     * @return oldest retained cycle, or empty when there is none
     */
    Optional<CycleRecord> oldestCycle(String agent);

    /**
     * Finds a cycle of an agent.
     *
     * @param agent name of the agent
     * @param cycle number of the cycle
     * @return cycle, or empty when not retained
     */
    Optional<CycleRecord> cycle(String agent, int cycle);

    /**
     * Lists the cycles of an agent in a range.
     *
     * @param agent name of the agent
     * @param from  first cycle, inclusive
     * @param to    last cycle, inclusive
     * @return retained cycles in the range
     */
    List<CycleRecord> cycles(String agent, int from, int to);

    /**
     * Lists the entities of an agent that exist in a cycle.
     *
     * @param agent name of the agent
     * @param cycle number of the cycle
     * @return entities existing in the cycle
     */
    List<Entity> entitiesAt(String agent, int cycle);

    /**
     * Lists the occurrences of an agent in a cycle.
     *
     * @param agent name of the agent
     * @param cycle number of the cycle
     * @return occurrences of the cycle
     */
    List<OccurrenceRecord> occurrencesAt(String agent, int cycle);

    /**
     * Lists the occurrences of an agent caused by an entity.
     *
     * @param agent  name of the agent
     * @param entity causing entity
     * @return occurrences caused by the entity
     */
    List<OccurrenceRecord> causedBy(String agent, Entity entity);

    /**
     * Lists the occurrences of an entity.
     *
     * @param agent  name of the agent
     * @param entity entity observed
     * @return occurrences of the entity
     */
    List<OccurrenceRecord> occurrencesOf(String agent, Entity entity);

    /**
     * Lists the plan selections of an agent in a cycle.
     *
     * @param agent name of the agent
     * @param cycle number of the cycle
     * @return plan selections of the cycle
     */
    List<PlanSelection> planSelectionsAt(String agent, int cycle);

    /**
     * Lists the moments in which a deed of a plan was executed.
     *
     * @param agent     name of the agent
     * @param plan      plan that contains the deed
     * @param deedIndex index of the deed in the plan body
     * @return moments of the executions
     */
    List<Moment> deedExecutions(String agent, Plan plan, int deedIndex);

    /**
     * Lists the entities of an agent with some types.
     *
     * @param agent name of the agent
     * @param types type names to keep
     * @return entities of the given types
     */
    List<Entity> entitiesOfTypes(String agent, Set<String> types);

    /**
     * Finds an entity by identifier.
     *
     * @param agent name of the agent
     * @param id    identifier of the entity
     * @return entity, or empty when unknown
     */
    Optional<Entity> entityById(String agent, long id);

    /**
     * Tells whether an entity was removed by pruning.
     *
     * @param agent name of the agent
     * @param id    identifier of the entity
     * @return true if the entity was pruned
     */
    boolean wasPruned(String agent, long id);

    /**
     * Returns all recorded messages.
     *
     * @return recorded messages
     */
    Collection<Message> messages();
}
