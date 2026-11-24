package jason.mind.api.core.ports.in.capture;

import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.domain.model.trace.Verdict;

import java.util.List;

/**
 * Input of the trace of a plan selection.
 *
 * @param agent        name of the agent
 * @param moment       moment of the selection
 * @param eventId      identifier of the event being handled
 * @param searchMethod method used to search the plans
 * @param evaluations  evaluations of the plans
 * @author Gabriel Lima
 */
public record RecordPlanSelectionIn(String agent, Moment moment, long eventId, PlanSelection.SearchMethod searchMethod,
                                    List<Evaluation> evaluations) {

    /**
     * Evaluation of the context of a plan.
     *
     * @param planId identifier of the plan
     * @param terms  evaluations of the context terms
     */
    public record Evaluation(long planId, List<Term> terms) {

    }

    /**
     * Evaluation of a context term.
     *
     * @param index    position of the term in the context
     * @param verdict  verdict of the term
     * @param bindings bindings produced, each as a name and value pair
     */
    public record Term(int index, Verdict verdict, List<String[]> bindings) {

    }
}
