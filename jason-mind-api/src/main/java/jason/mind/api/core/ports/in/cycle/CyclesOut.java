package jason.mind.api.core.ports.in.cycle;

import java.util.List;

public record CyclesOut(String agent, Integer oldestCycle, Integer newestCycle, Integer from, Integer to, List<CycleItem> cycles) {

    public record CycleItem(int cycle, Phases phases) {
    }

    public record Phases(boolean sense, boolean deliberate, boolean act) {
    }
}
