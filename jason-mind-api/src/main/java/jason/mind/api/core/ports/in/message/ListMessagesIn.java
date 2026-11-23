package jason.mind.api.core.ports.in.message;

import java.util.Set;

public record ListMessagesIn(Set<String> agents, Set<String> senders, Set<String> receivers, Long until, String after,
                             String before, Integer limit) {
}
