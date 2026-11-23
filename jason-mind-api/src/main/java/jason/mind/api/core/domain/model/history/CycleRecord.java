package jason.mind.api.core.domain.model.history;

public record CycleRecord(int cycle, long timeEndMilliseconds, long sequenceEnd, boolean sense, boolean deliberate, boolean act) {
}
