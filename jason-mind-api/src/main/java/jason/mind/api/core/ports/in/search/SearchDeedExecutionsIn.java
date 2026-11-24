package jason.mind.api.core.ports.in.search;

/**
 * Input of the search of the executions of a deed.
 *
 * @param agent     name of the agent
 * @param plan      identifier of the plan
 * @param deedIndex index of the deed, or null
 * @author Gabriel Lima
 */
public record SearchDeedExecutionsIn(String agent, String plan, Integer deedIndex) {

}
