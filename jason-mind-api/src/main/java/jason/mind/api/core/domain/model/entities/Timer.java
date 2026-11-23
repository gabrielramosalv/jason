package jason.mind.api.core.domain.model.entities;

import java.util.Map;

import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;

public final class Timer extends Entity<Timer> {

    private final Trigger trigger;
    private final long deadlineMilliseconds;

    public Timer(long id, Occurrence start, Trigger trigger, long deadlineMilliseconds) {
        super(id, start);
        this.trigger = trigger;
        this.deadlineMilliseconds = deadlineMilliseconds;
    }

    public Trigger getTrigger() { return trigger; }

    public long getDeadlineMilliseconds() { return deadlineMilliseconds; }

    @Override public Map<String, Timeline<?>> timelines() { return Map.of(); }

    @Override public String typeName() { return "timer"; }

    @Override public String label() { return trigger.text(); }
}
