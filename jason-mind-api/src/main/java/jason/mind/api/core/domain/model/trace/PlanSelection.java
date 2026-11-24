package jason.mind.api.core.domain.model.trace;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.timeline.Moment;

import java.util.List;
import java.util.Objects;

/**
 * Trace of the plan selection for an event.
 * Holds when it happened, the event, the search method and the evaluations of the plans.
 *
 * @param moment       moment of the selection
 * @param sequence     global sequence number of the selection
 * @param event        event being handled
 * @param searchMethod method used to search the plans
 * @param evaluations  evaluations of the relevant plans
 * @author Gabriel Lima
 */
public record PlanSelection(Moment moment, long sequence, Event event, SearchMethod searchMethod,
                            List<PlanEvaluation> evaluations) {

    /**
     * Validates the selection and copies the evaluations, using an empty list when they are null.
     *
     * @throws NullPointerException if the moment, the event or the search method is null
     */
    public PlanSelection {
        Objects.requireNonNull(moment, "moment");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(searchMethod, "searchMethod");
        evaluations = evaluations == null ? List.of() : List.copyOf(evaluations);
    }

    /**
     * Method used to search the plans.
     */
    public enum SearchMethod {
        /** Stops at the first applicable plan. */
        FIRST_FOUND,
        /** Evaluates all relevant plans and collects the applicable ones. */
        RELEVANT_AND_APPLICABLE
    }
}
