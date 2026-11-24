package jason.mind.api.core.domain.service.reasoning;

/**
 * Rule that explains the outcome of a candidate in a selection step.
 * Covers the event, option and intention selections.
 *
 * @author Gabriel Lima
 */
public enum SelectionRule {

    /** The chosen event was the first of the queue. */
    SELECT_EVENT_FIFO,

    /** The chosen event was atomic. */
    SELECT_EVENT_ATOMIC_PRIORITY,

    /** The event was chosen by a custom selection function. */
    SELECT_EVENT_CUSTOM,

    /** The plan context was false. */
    PLAN_NOT_APPLICABLE,

    /** The chosen plan was the first applicable one. */
    PLAN_FIRST_FOUND,

    /** The plan came after the chosen one and was not evaluated. */
    PLAN_NOT_EVALUATED,

    /** The plan had no evaluation. */
    PLAN_NOT_RELEVANT,

    /** The plan was applicable but not chosen. */
    PLAN_NOT_SELECTED,

    /** The plan was chosen by a custom selection function. */
    PLAN_CUSTOM,

    /** The chosen intention was the first of the queue. */
    SELECT_INTENTION_ROUND_ROBIN
}
