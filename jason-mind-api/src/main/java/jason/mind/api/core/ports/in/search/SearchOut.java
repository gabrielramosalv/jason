package jason.mind.api.core.ports.in.search;

import java.util.List;

public record SearchOut(String agent, String query, Window window, Integer total, List<Hit> hits) {

    public record Window(Integer oldestCycle, Integer newestCycle) {
    }

    public record Hit(String type, String entityId, String label, int firstCycle, Integer lastCycle) {
    }
}
