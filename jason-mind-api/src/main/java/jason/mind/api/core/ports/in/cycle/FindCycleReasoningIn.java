package jason.mind.api.core.ports.in.cycle;

/**
 * Input of the query of the reasoning of an agent in a cycle.
 *
 * @param agent name of the agent
 * @param cycle number of the cycle
 * @author Gabriel Lima
 */
public record FindCycleReasoningIn(String agent, int cycle) {

}
