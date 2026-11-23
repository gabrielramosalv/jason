package jason.mind.api.core.domain.model.entities;

import java.util.LinkedHashMap;
import java.util.Map;

import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;

public final class Event extends Entity<Event> {

    public enum Place { QUEUE, PENDING, SELECTED }

    public enum State { PENDING, SELECTED, NO_APPLICABLE }

    private final boolean atomic;
    private final Timeline<Trigger> trigger = new Timeline<>();
    private final Timeline<Place> place = new Timeline<>();
    private final Timeline<State> state = new Timeline<>();

    public Event(long id, Occurrence start, Trigger trigger, boolean atomic, Place place) {
        super(id, start);
        this.atomic = atomic;
        this.trigger.put(start, trigger);
        this.place.put(start, place);
        this.state.put(start, State.PENDING);
    }

    public boolean isAtomic() { return atomic; }

    public Timeline<Trigger> getTrigger() { return trigger; }

    public Timeline<Place> getPlace() { return place; }

    public Timeline<State> getState() { return state; }

    public Trigger getBirthTrigger() {
        return trigger.entries().get(0).value();
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        Map<String, Timeline<?>> m = new LinkedHashMap<>();
        m.put("trigger", trigger);
        m.put("place", place);
        m.put("state", state);
        return m;
    }

    @Override public String typeName() { return "event"; }

    @Override public String label() { return trigger.current().text(); }
}
