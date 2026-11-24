package jason.mind.api.core.domain.model.history;

/**
 * Reasoning configuration of an agent.
 * Holds the cycle parameters, the event policy and which selection functions keep their default behavior.
 *
 * @param nrcbp                  number of reasoning cycles before perception
 * @param cyclesSense            number of sense steps per cycle
 * @param cyclesDeliberate       number of deliberate steps per cycle
 * @param cyclesAct              number of act steps per cycle
 * @param eventsPolicy           policy applied to events without applicable plans
 * @param tro                    whether tail recursion optimization is enabled
 * @param sync                   whether the agent runs synchronously
 * @param selectEventDefault     whether the event selection function is the default one
 * @param selectOptionDefault    whether the option selection function is the default one
 * @param selectIntentionDefault whether the intention selection function is the default one
 * @param selectMessageDefault   whether the message selection function is the default one
 * @param selectActionDefault    whether the action selection function is the default one
 * @param agentClass             fully qualified name of the agent class
 * @author Gabriel Lima
 */
public record AgentConfig(int nrcbp, int cyclesSense, int cyclesDeliberate, int cyclesAct, String eventsPolicy,
                          boolean tro, boolean sync, boolean selectEventDefault, boolean selectOptionDefault,
                          boolean selectIntentionDefault, boolean selectMessageDefault, boolean selectActionDefault,
                          String agentClass) {

    /**
     * Returns the default configuration.
     *
     * @return configuration with the default values of every parameter
     */
    public static AgentConfig defaults() {
        return new AgentConfig(1, 1, 1, 1, "discard", true, false, true, true, true, true, true,
                "jason.asSemantics.Agent");
    }
}
