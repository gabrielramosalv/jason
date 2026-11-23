package jason.mind.api.core.domain.model.trace;

import java.util.List;
import java.util.Objects;

import jason.mind.api.core.domain.model.entities.Plan;

public record PlanEvaluation(Plan plan, List<TermEvaluation> context) {

    public PlanEvaluation {
        Objects.requireNonNull(plan, "plan");
        context = context == null ? List.of() : List.copyOf(context);
    }

    public boolean isApplicable() {
        if (context.isEmpty()) return true;
        Verdict last = context.get(context.size() - 1).result();
        return last == Verdict.TRUE || last == Verdict.MIXED;
    }
}
