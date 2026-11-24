package jason.mind.api.core.ports.in.agent;

import jason.mind.api.core.ports.in.history.EntityRefOut;

import java.util.List;

/**
 * Output with the mental state of an agent at a cycle.
 * Lists the beliefs, intentions, events and intended means alive at that cycle.
 *
 * @param agent         name of the agent
 * @param cycle         cycle observed, or null when there is none
 * @param clamped       whether the requested cycle was moved into the retained window
 * @param oldestCycle   oldest retained cycle, or null
 * @param newestCycle   newest closed cycle, or null
 * @param sequenceEnd   last sequence of the observed cycle, or null
 * @param beliefs       beliefs alive at the cycle
 * @param intentions    intentions alive at the cycle
 * @param events        events alive at the cycle
 * @param intendedMeans intended means alive at the cycle
 * @author Gabriel Lima
 */
public record AgentStateOut(String agent, Integer cycle, boolean clamped, Integer oldestCycle, Integer newestCycle,
                            Long sequenceEnd, List<BeliefOut> beliefs, List<IntentionOut> intentions,
                            List<EventOut> events, List<IntendedMeansOut> intendedMeans) {

    /**
     * Belief in the agent state.
     *
     * @param id          identifier of the belief
     * @param content     text of the belief
     * @param annotations annotations of the belief
     * @param categories  categories of the belief sources
     * @param change      change the belief went through in the cycle, or null
     */
    public record BeliefOut(String id, String content, List<String> annotations, List<String> categories,
                            String change) {

    }

    /**
     * Intention in the agent state.
     *
     * @param id              identifier of the intention
     * @param state           state of the intention
     * @param executedInCycle whether the intention was executed in the cycle
     * @param waitingOn       entity the intention waits on, or null
     * @param levels          levels of the intention stack
     */
    public record IntentionOut(String id, String state, boolean executedInCycle, EntityRefOut waitingOn,
                               List<LevelOut> levels) {

    }

    /**
     * Level of an intention stack.
     *
     * @param intendedMeansId identifier of the intended means at this level
     * @param kind            kind of the level
     * @param trigger         text of the trigger of the level
     */
    public record LevelOut(String intendedMeansId, String kind, String trigger) {

    }

    /**
     * Event in the agent state.
     *
     * @param id      identifier of the event
     * @param trigger text of the trigger
     * @param atomic  whether the event is atomic
     * @param place   place of the event
     * @param state   processing state of the event
     * @param change  change the event went through in the cycle, or null
     */
    public record EventOut(String id, String trigger, boolean atomic, String place, String state, String change) {

    }

    /**
     * Intended means in the agent state.
     *
     * @param id          identifier of the intended means
     * @param intentionId identifier of the owning intention
     * @param planId      identifier of the instantiated plan
     * @param currentDeed index of the deed being executed, or null
     * @param bindings    variable bindings
     */
    public record IntendedMeansOut(String id, String intentionId, String planId, Integer currentDeed,
                                   List<BindingOut> bindings) {

    }

    /**
     * Variable binding of an intended means.
     *
     * @param name  name of the variable
     * @param value bound value
     * @param line  source line where the binding was made, or null
     */
    public record BindingOut(String name, String value, Integer line) {

    }
}
