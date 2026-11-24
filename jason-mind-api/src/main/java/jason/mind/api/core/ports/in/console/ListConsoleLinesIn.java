package jason.mind.api.core.ports.in.console;

import java.util.Set;

/**
 * Input of the listing of console lines.
 *
 * @param agents agents whose lines are listed, or null for all
 * @param until  last sequence considered, or null
 * @param after  cursor after which the page starts, or null
 * @param before cursor before which the page ends, or null
 * @param limit  maximum number of lines, or null
 * @author Gabriel Lima
 */
public record ListConsoleLinesIn(Set<String> agents, Long until, Long after, Long before, Integer limit) {

}
