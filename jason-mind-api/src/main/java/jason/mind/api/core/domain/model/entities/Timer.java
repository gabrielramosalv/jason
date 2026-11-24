package jason.mind.api.core.domain.model.entities;

import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;
import lombok.Getter;

import java.util.Map;

/**
 * Timer scheduled by an agent.
 * Keeps the trigger it fires and its deadline.
 *
 * @author Gabriel Lima
 */
@Getter
public final class Timer extends Entity {

    /** Trigger fired when the timer expires. */
    private final Trigger trigger;

    /** Deadline of the timer, in milliseconds. */
    private final long deadlineMilliseconds;

    /**
     * Creates the timer with its identifier, start occurrence, trigger and deadline.
     *
     * @param id                   unique identifier of the entity
     * @param start                occurrence that scheduled the timer
     * @param trigger              {@link #trigger}
     * @param deadlineMilliseconds {@link #deadlineMilliseconds}
     */
    public Timer(long id, Occurrence start, Trigger trigger, long deadlineMilliseconds) {
        super(id, start);
        this.trigger = trigger;
        this.deadlineMilliseconds = deadlineMilliseconds;
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        return Map.of();
    }

    @Override
    public String typeName() {
        return "timer";
    }

    @Override
    public String label() {
        return this.trigger.text();
    }
}
