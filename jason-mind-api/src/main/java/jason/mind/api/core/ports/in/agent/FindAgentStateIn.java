package jason.mind.api.core.ports.in.agent;

import java.util.Set;

/**
 * Input of the query of the mental state of an agent.
 *
 * @param agent   name of the agent
 * @param cycle   cycle to observe, or null for the newest
 * @param include sections to include in the output, or null for all
 * @author Gabriel Lima
 */
public record FindAgentStateIn(String agent, Integer cycle, Set<String> include) {

}
