package jason.mind.api.core.domain.model.trace;

import jason.mind.api.core.domain.model.entities.Plan;

import java.util.List;
import java.util.Objects;

/**
 * Evaluation of the context of a plan.
 * Holds the plan and the verdict of each context term.
 *
 * @param plan    evaluated plan
 * @param context evaluations of the context terms, in order
 * @author Gabriel Lima
 */
public record PlanEvaluation(Plan plan, List<TermEvaluation> context) {

    /**
     * Validates the evaluation and copies the context, using an empty list when it is null.
     *
     * @throws NullPointerException if the plan is null
     */
    public PlanEvaluation {
        Objects.requireNonNull(plan, "plan");
        context = context == null ? List.of() : List.copyOf(context);
    }

    /**
     * Tells whether the plan is applicable.
     *
     * @return true if the context is empty or its last term is true or mixed
     */
    public boolean isApplicable() {
        if (this.context.isEmpty()) {
            return true;
        }
        Verdict last = this.context.get(this.context.size() - 1).result();
        return last == Verdict.TRUE || last == Verdict.MIXED;
    }
}
