package jason.mind.api.core.domain.model.entities;

/**
 * Mode of a message send operation.
 * Tells whether the sender waits for an answer.
 *
 * @author Gabriel Lima
 */
public enum SendMode {
    /** The sender does not wait for an answer. */
    ASYNCHRONOUS,
    /** The sender waits for an answer. */
    SYNCHRONOUS
}
