package jason.mind.api.core.ports.in.message;

import java.util.Set;

/**
 * Input of the listing of messages.
 *
 * @param agents    agents involved, or null for all
 * @param senders   senders to keep, or null for all
 * @param receivers receivers to keep, or null for all
 * @param until     last sequence considered, or null
 * @param after     cursor after which the page starts, or null
 * @param before    cursor before which the page ends, or null
 * @param limit     maximum number of messages, or null
 * @author Gabriel Lima
 */
public record ListMessagesIn(Set<String> agents, Set<String> senders, Set<String> receivers, Long until, String after,
                             String before, Integer limit) {

}
