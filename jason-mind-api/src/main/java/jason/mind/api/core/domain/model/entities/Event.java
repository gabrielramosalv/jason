package jason.mind.api.core.domain.model.entities;

import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.AppendOnlyList;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Event generated for an agent.
 * Keeps the timelines of its trigger, of where it is placed and of its processing state, and the plan selections
 * made for it.
 *
 * @author Gabriel Lima
 */
@Getter
public final class Event extends Entity {

    /** Whether the event is atomic. */
    private final boolean atomic;

    /** Timeline of the event trigger. */
    private final Timeline<Trigger> trigger = new Timeline<>();

    /** Timeline of the place of the event. */
    private final Timeline<Place> place = new Timeline<>();

    /** Timeline of the processing state of the event. */
    private final Timeline<State> state = new Timeline<>();

    /** Plan selections made for the event, in recording order, or null until the first one. */
    private volatile AppendOnlyList<PlanSelection> selections;

    /**
     * Creates the event with its identifier, start occurrence, trigger, atomicity and initial place.
     * The state starts as pending.
     *
     * @param id      unique identifier of the entity
     * @param start   occurrence that generated the event
     * @param trigger initial trigger of the event
     * @param atomic  {@link #atomic}
     * @param place   initial place of the event
     */
    public Event(long id, Occurrence start, Trigger trigger, boolean atomic, Place place) {
        super(id, start);
        this.atomic = atomic;
        this.trigger.put(start, trigger);
        this.place.put(start, place);
        this.state.put(start, State.PENDING);
    }

    /**
     * Returns the trigger the event had when it was generated.
     *
     * @return first trigger of the timeline
     */
    public Trigger getBirthTrigger() {
        return this.trigger.entries().get(0).value();
    }

    /**
     * Appends a plan selection made for the event.
     *
     * @param selection plan selection whose event is this one
     */
    public void addSelection(PlanSelection selection) {
        AppendOnlyList<PlanSelection> l = this.selections;
        if (l == null) {
            l = new AppendOnlyList<>();
            this.selections = l;
        }
        l.add(selection);
    }

    /**
     * Returns the plan selections made for the event.
     *
     * @return snapshot of the plan selections, in recording order
     */
    public List<PlanSelection> getSelections() {
        AppendOnlyList<PlanSelection> l = this.selections;
        return l == null ? List.of() : l.snapshot();
    }

    /**
     * Drops the plan selections made before the horizon cycle.
     *
     * @param horizonCycle first cycle whose plan selections are kept
     */
    public void pruneSelectionsBefore(int horizonCycle) {
        AppendOnlyList<PlanSelection> l = this.selections;
        if (l == null) {
            return;
        }
        List<PlanSelection> all = l.snapshot();
        List<PlanSelection> kept = new ArrayList<>(all.size());
        for (PlanSelection s : all) {
            if (s.moment().cycle() >= horizonCycle) {
                kept.add(s);
            }
        }
        if (kept.size() != all.size()) {
            l.replaceAll(kept);
        }
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        Map<String, Timeline<?>> m = new LinkedHashMap<>();
        m.put("trigger", this.trigger);
        m.put("place", this.place);
        m.put("state", this.state);
        return m;
    }

    @Override
    public String typeName() {
        return "event";
    }

    @Override
    public String label() {
        return this.trigger.current().text();
    }

    /**
     * Place where an event is located in the agent circumstance.
     * Tells whether the event waits in the queue, is pending or was selected.
     */
    public enum Place {
        /** The event is in the event queue. */
        QUEUE,
        /** The event is among the pending events. */
        PENDING,
        /** The event is the selected event. */
        SELECTED
    }

    /**
     * Processing state of an event.
     * Tells whether the event is pending, was selected or had no applicable plan.
     */
    public enum State {
        /** The event was not handled yet. */
        PENDING,
        /** The event was selected for handling. */
        SELECTED,
        /** No applicable plan was found for the event. */
        NO_APPLICABLE
    }
}
