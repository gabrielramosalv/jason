package jason.mind.api.core.domain.service.history;

import jason.mind.api.core.domain.exception.history.InvariantViolationException;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;

/**
 * Domain service that checks the invariants of a new occurrence before it is recorded.
 *
 * @author Gabriel Lima
 */
public final class HistoryRecorderDomainService {

    /**
     * Checks that an occurrence can be recorded.
     *
     * @param agent        agent of the occurrence, or null when unknown
     * @param target       entity the occurrence happens to, or null
     * @param occurrence   occurrence to check
     * @param lastSequence last sequence recorded so far
     * @throws InvariantViolationException if the agent is unknown or ended, the sequence does not grow,
     *                                     the target already ended or the cause ended before the occurrence
     */
    public void check(Agent agent, Entity target, Occurrence occurrence, long lastSequence) {
        if (agent == null) {
            throw new InvariantViolationException("INV-034", "occurrence of unknown agent: " + occurrence.agent());
        }
        if (agent.hasEnded()) {
            throw new InvariantViolationException("INV-034", "occurrence after the end of agent " + agent.getName());
        }
        if (occurrence.sequence() <= lastSequence) {
            throw new InvariantViolationException("INV-037",
                    "sequence " + occurrence.sequence() + " does not grow after " + lastSequence);
        }
        if (target != null && target.getEnd() != null) {
            throw new InvariantViolationException("INV-029", "occurrence on an ended entity: " + target);
        }
        if (occurrence.cause() != null) {
            Entity by = occurrence.cause().by();
            if (by.getEnd() != null && by.getEnd().sequence() < occurrence.sequence() && !this.causeMayHaveEnded(by,
                    occurrence)) {
                throw new InvariantViolationException("INV-029", "cause.by " + by + " ended before the occurrence");
            }
        }
    }

    /**
     * Tells whether an ended cause is still acceptable: messages always are, other entities only when they
     * ended at the same moment of the same agent.
     *
     * @param by         entity that caused the occurrence
     * @param occurrence occurrence being checked
     * @return true if the ended cause is acceptable
     */
    private boolean causeMayHaveEnded(Entity by, Occurrence occurrence) {
        Occurrence end = by.getEnd();
        if (by instanceof Message) {
            return true;
        }
        return end.agent().equals(occurrence.agent()) && end.moment().equals(occurrence.moment());
    }
}
