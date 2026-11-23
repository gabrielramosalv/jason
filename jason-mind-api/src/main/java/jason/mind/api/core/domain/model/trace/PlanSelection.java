package jason.mind.api.core.domain.model.trace;

import java.util.List;
import java.util.Objects;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.timeline.Moment;

public record PlanSelection(Moment moment, long sequence, Event event, SearchMethod searchMethod, List<PlanEvaluation> evaluations) {

    public enum SearchMethod { FIRST_FOUND, RELEVANT_AND_APPLICABLE }

    public PlanSelection {
        Objects.requireNonNull(moment, "moment");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(searchMethod, "searchMethod");
        evaluations = evaluations == null ? List.of() : List.copyOf(evaluations);
    }
}
