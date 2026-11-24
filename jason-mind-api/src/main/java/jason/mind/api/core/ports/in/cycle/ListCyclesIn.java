package jason.mind.api.core.ports.in.cycle;

/**
 * Input of the listing of the cycles of an agent.
 *
 * @param agent name of the agent
 * @param from  first cycle of the range, or null
 * @param to    last cycle of the range, or null
 * @author Gabriel Lima
 */
public record ListCyclesIn(String agent, Integer from, Integer to) {

}
