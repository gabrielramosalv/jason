package jason.mind.api.core.ports.in.cycle;

import java.util.List;

import jason.mind.api.core.ports.in.history.EntityRefOut;

public record CycleReasoningOut(String agent, int cycle, List<StepOut> steps) {

    public record StepOut(String step, String phase, int iteration, int ordinal, String searchMethod, List<EntityRefOut> inputs,
                          EntityRefOut chosen, List<CandidateOut> candidates, List<EntityRefOut> created,
                          List<ChangeOut> changed, List<EntityRefOut> removed) {
    }

    public record CandidateOut(String type, String id, String label, String rule, boolean chosen, List<TermOut> terms) {
    }

    public record TermOut(int index, String verdict, List<BindingOut> bindings) {
    }

    public record BindingOut(String name, String value) {
    }

    public record ChangeOut(String type, String id, String label, String timeline, String value) {
    }
}
