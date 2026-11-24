package jason.mind.api.core.ports.in.prune;

/**
 * Input of the manual prune.
 *
 * @param agent name of the agent, or null for every agent
 * @param until oldest cycle to keep; the cycles before it are removed
 * @author Gabriel Lima
 */
public record RequestPruneIn(String agent, Integer until) {

}
