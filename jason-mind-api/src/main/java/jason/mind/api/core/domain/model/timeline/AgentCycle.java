package jason.mind.api.core.domain.model.timeline;

import java.util.Objects;

public record AgentCycle(String agent, int cycle) {
    public AgentCycle {
        Objects.requireNonNull(agent, "agent");
    }
}
