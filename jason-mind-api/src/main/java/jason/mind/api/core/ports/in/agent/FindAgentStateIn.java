package jason.mind.api.core.ports.in.agent;

import java.util.Set;

public record FindAgentStateIn(String agent, Integer cycle, Set<String> include) {
}
