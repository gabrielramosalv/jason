package jason.mind.api.core.ports.in.mas;

import java.util.List;

public record MasOut(String mas, Versions versions, List<AgentItem> agents) {

    public record Versions(String mind, String jason) {
    }

    public record AgentItem(String name, boolean active, boolean ended, Integer oldestCycle, Integer newestCycle) {
    }
}
