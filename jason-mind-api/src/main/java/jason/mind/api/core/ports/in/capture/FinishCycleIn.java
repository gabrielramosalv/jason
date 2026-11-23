package jason.mind.api.core.ports.in.capture;

public record FinishCycleIn(String agent, int cycle, boolean sense, boolean deliberate, boolean act) {
}
