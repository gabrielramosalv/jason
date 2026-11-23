package jason.mind.api.core.ports.in.plan;

import java.util.List;

public record PlansOut(String agent, long version, List<PlanOut> plans) {

    public record PlanOut(String id, String label, TriggerOut trigger, List<String> context, SourceOut source,
                          List<String> annotations, long orderKey, Alive alive, List<DeedOut> body) {
    }

    public record TriggerOut(String operator, String type, String literal) {
    }

    public record SourceOut(String path, Integer line) {
    }

    public record Alive(int fromCycle, Integer toCycle) {
    }

    public record DeedOut(String type, String content, Integer line, int parent) {
    }
}
