package jason.mind.api.core.ports.in.console;

import java.util.Set;

public record ListConsoleLinesIn(Set<String> agents, Long until, Long after, Long before, Integer limit) {
}
