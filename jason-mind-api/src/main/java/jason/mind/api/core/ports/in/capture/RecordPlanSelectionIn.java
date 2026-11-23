package jason.mind.api.core.ports.in.capture;

import java.util.List;

import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.domain.model.trace.Verdict;

public record RecordPlanSelectionIn(String agent, Moment moment, long eventId, PlanSelection.SearchMethod searchMethod,
                                    List<Evaluation> evaluations) {

    public record Evaluation(long planId, List<Term> terms) {
    }

    public record Term(int index, Verdict verdict, List<String[]> bindings) {
    }
}
