package jason.mind.api.core.domain.model.history;

public record AgentConfig(int nrcbp, int cyclesSense, int cyclesDeliberate, int cyclesAct, String eventsPolicy,
                          boolean tro, boolean sync, boolean selectEventDefault, boolean selectOptionDefault,
                          boolean selectIntentionDefault, boolean selectMessageDefault, boolean selectActionDefault,
                          String agentClass) {

    public static AgentConfig defaults() {
        return new AgentConfig(1, 1, 1, 1, "discard", true, false, true, true, true, true, true, "jason.asSemantics.Agent");
    }
}
