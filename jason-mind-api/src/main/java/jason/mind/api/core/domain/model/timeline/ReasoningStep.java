package jason.mind.api.core.domain.model.timeline;

public enum ReasoningStep {
    PERCEIVE(ReasoningPhase.SENSE),
    PROCESS_MESSAGES(ReasoningPhase.SENSE),
    CYCLE_START_TASKS(ReasoningPhase.DELIBERATE),
    SELECT_EVENT(ReasoningPhase.DELIBERATE),
    FIND_OPTION(ReasoningPhase.DELIBERATE),
    ADD_INTENDED_MEANS(ReasoningPhase.DELIBERATE),
    PROCESS_ACTIONS(ReasoningPhase.ACT),
    SELECT_INTENTION(ReasoningPhase.ACT),
    EXECUTE_INTENTION(ReasoningPhase.ACT),
    CLEAR_INTENTION(ReasoningPhase.ACT),
    INITIAL_LOAD(ReasoningPhase.EXTERNAL),
    OUT_OF_CYCLE(ReasoningPhase.EXTERNAL);

    private final ReasoningPhase phase;

    ReasoningStep(ReasoningPhase phase) {
        this.phase = phase;
    }

    public ReasoningPhase getPhase() {
        return phase;
    }
}
