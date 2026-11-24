package jason.mind.api.core.domain.model.timeline;

/**
 * Phase of the reasoning cycle.
 *
 * @author Gabriel Lima
 */
public enum ReasoningPhase {
    /** Perception and message handling. */
    SENSE,
    /** Event and plan selection. */
    DELIBERATE,
    /** Intention selection and execution. */
    ACT,
    /** Outside the reasoning cycle. */
    EXTERNAL
}
