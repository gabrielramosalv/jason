package jason.mind.api.core.ports.in.agent;

import java.util.List;

import jason.mind.api.core.ports.in.history.EntityRefOut;

public record AgentStateOut(String agent, Integer cycle, boolean clamped, Integer oldestCycle, Integer newestCycle,
                            Long sequenceEnd, List<BeliefOut> beliefs, List<IntentionOut> intentions,
                            List<EventOut> events, List<IntendedMeansOut> intendedMeans) {

    public record BeliefOut(String id, String content, List<String> annotations, List<String> categories, String change) {
    }

    public record IntentionOut(String id, String state, boolean executedInCycle, EntityRefOut waitingOn, List<LevelOut> levels) {
    }

    public record LevelOut(String intendedMeansId, String kind, String trigger) {
    }

    public record EventOut(String id, String trigger, boolean atomic, String place, String state, String change) {
    }

    public record IntendedMeansOut(String id, String intentionId, String planId, Integer currentDeed, List<BindingOut> bindings) {
    }

    public record BindingOut(String name, String value, Integer line) {
    }
}
