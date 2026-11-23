package jason.mind.api.core.domain.service.history;

import jason.mind.api.core.domain.exception.history.InvariantViolationException;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;

public final class HistoryRecorderDomainService {

    public void check(Agent agent, Entity<?> target, Occurrence occurrence, long lastSequence) {
        if (agent == null)
            throw new InvariantViolationException("INV-034", "occurrence of unknown agent: " + occurrence.agent());
        if (agent.hasEnded())
            throw new InvariantViolationException("INV-034", "occurrence after the end of agent " + agent.getName());
        if (occurrence.sequence() <= lastSequence)
            throw new InvariantViolationException("INV-037", "sequence " + occurrence.sequence() + " does not grow after " + lastSequence);
        if (target != null && target.getEnd() != null)
            throw new InvariantViolationException("INV-029", "occurrence on an ended entity: " + target);
        if (occurrence.cause() != null) {
            Entity<?> by = occurrence.cause().by();
            if (by.getEnd() != null && by.getEnd().sequence() < occurrence.sequence() && !causeMayHaveEnded(by, occurrence))
                throw new InvariantViolationException("INV-029", "cause.by " + by + " ended before the occurrence");
        }
    }

    private boolean causeMayHaveEnded(Entity<?> by, Occurrence occurrence) {
        Occurrence end = by.getEnd();
        if (by instanceof Message) return true;
        return end.agent().equals(occurrence.agent()) && end.moment().equals(occurrence.moment());
    }
}
