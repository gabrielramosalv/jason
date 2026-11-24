package jason.mind.api.core.domain.model.entities;

import jason.mind.api.core.domain.model.timeline.AppendOnlyList;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Intention of an agent.
 * Keeps the timelines of its place, suspension and awaited entity, plus its stack of intended means.
 *
 * @author Gabriel Lima
 */
public final class Intention extends Entity {

    /** Timeline of the place of the intention. */
    @Getter
    private final Timeline<Place> place = new Timeline<>();

    /** Timeline of whether the intention is suspended. */
    @Getter
    private final Timeline<Boolean> suspended = new Timeline<>();

    /** Timeline of the entity the intention waits on. */
    @Getter
    private final Timeline<Entity> waitingOn = new Timeline<>();

    /** Stack of intended means, from bottom to top. */
    private final AppendOnlyList<IntendedMeans> stack = new AppendOnlyList<>();

    /**
     * Creates the intention with its identifier, start occurrence and initial place.
     * The intention starts not suspended.
     *
     * @param id    unique identifier of the entity
     * @param start occurrence that created the intention
     * @param place initial place of the intention
     */
    public Intention(long id, Occurrence start, Place place) {
        super(id, start);
        this.place.put(start, place);
        this.suspended.put(start, Boolean.FALSE);
    }

    /**
     * Derives the state of an intention. Suspension has priority, then the awaited entity, then the place.
     *
     * @param place     place of the intention
     * @param suspended whether the intention is suspended
     * @param waitingOn entity the intention waits on, or null
     * @return derived state
     */
    public static State stateOf(Place place, Boolean suspended, Entity waitingOn) {
        if (Boolean.TRUE.equals(suspended)) {
            return State.SUSPENDED;
        }
        if (waitingOn instanceof Action) {
            return State.WAITING_ACTION;
        }
        if (waitingOn instanceof Event) {
            return State.WAITING_EVENT;
        }
        if (place == Place.SELECTED_INTENTION) {
            return State.SELECTED;
        }
        if (place == Place.PENDING_INTENTIONS) {
            return State.PENDING;
        }
        return State.QUEUED;
    }

    /**
     * Pushes an intended means on top of the stack.
     *
     * @param intendedMeans intended means to push
     */
    public void push(IntendedMeans intendedMeans) {
        this.stack.add(intendedMeans);
    }

    /**
     * Removes from the stack the intended means that ended before the horizon cycle.
     *
     * @param horizonCycle first cycle whose ended intended means are kept
     */
    public void pruneStack(int horizonCycle) {
        List<IntendedMeans> all = this.stack.snapshot();
        List<IntendedMeans> kept = new ArrayList<>(all.size());
        for (IntendedMeans im : all) {
            if (im.getEnd() == null || im.getEnd().cycle() >= horizonCycle) {
                kept.add(im);
            }
        }
        if (kept.size() != all.size()) {
            this.stack.replaceAll(kept);
        }
    }

    /**
     * Returns the intended means alive at the given sequence.
     *
     * @param sequenceEnd sequence at which the stack is observed
     * @return intended means started at or before the sequence and not ended at it
     */
    public List<IntendedMeans> stackAt(long sequenceEnd) {
        List<IntendedMeans> r = new ArrayList<>();
        for (IntendedMeans im : this.stack.snapshot()) {
            if (im.getStart().sequence() <= sequenceEnd && (im.getEnd() == null
                    || im.getEnd().sequence() > sequenceEnd)) {
                r.add(im);
            }
        }
        return r;
    }

    /**
     * Returns a snapshot of the stack of intended means.
     *
     * @return intended means from bottom to top
     */
    public List<IntendedMeans> getStack() {
        return this.stack.snapshot();
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        Map<String, Timeline<?>> m = new LinkedHashMap<>();
        m.put("place", this.place);
        m.put("suspended", this.suspended);
        m.put("waitingOn", this.waitingOn);
        return m;
    }

    @Override
    public String typeName() {
        return "intention";
    }

    @Override
    public String label() {
        if (this.getStart().cause() != null && this.getStart().cause().by() instanceof Event e) {
            return e.getTrigger().before(this.getStart().sequence() + 1).text();
        }
        List<IntendedMeans> s = this.stack.snapshot();
        return s.isEmpty() ? "#" + this.getId() : s.get(0).label();
    }

    /**
     * Place where an intention is located in the agent circumstance.
     */
    public enum Place {
        /** The intention is the selected intention. */
        SELECTED_INTENTION,
        /** The intention is among the pending intentions. */
        PENDING_INTENTIONS,
        /** The intention is in the set of running intentions. */
        RUNNING,
        /** The intention is in no place. */
        NONE
    }

    /**
     * State of an intention derived from its place, suspension and awaited entity.
     */
    public enum State {
        /** The intention waits to be selected. */
        QUEUED,
        /** The intention is selected. */
        SELECTED,
        /** The intention waits for an action. */
        WAITING_ACTION,
        /** The intention waits for an event. */
        WAITING_EVENT,
        /** The intention is pending. */
        PENDING,
        /** The intention is suspended. */
        SUSPENDED
    }
}
