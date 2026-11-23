package jason.mind.api.core.domain.model.entities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jason.mind.api.core.domain.model.timeline.AppendOnlyList;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;

public final class Intention extends Entity<Intention> {

    public enum Place { SELECTED_INTENTION, PENDING_INTENTIONS, RUNNING, NONE }

    public enum State { QUEUED, SELECTED, WAITING_ACTION, WAITING_EVENT, PENDING, SUSPENDED }

    private final Timeline<Place> place = new Timeline<>();
    private final Timeline<Boolean> suspended = new Timeline<>();
    private final Timeline<Entity<?>> waitingOn = new Timeline<>();
    private final AppendOnlyList<IntendedMeans> stack = new AppendOnlyList<>();

    public Intention(long id, Occurrence start, Place place) {
        super(id, start);
        this.place.put(start, place);
        this.suspended.put(start, Boolean.FALSE);
    }

    public Timeline<Place> getPlace() { return place; }

    public Timeline<Boolean> getSuspended() { return suspended; }

    public Timeline<Entity<?>> getWaitingOn() { return waitingOn; }

    public List<IntendedMeans> getStack() { return stack.snapshot(); }

    public void push(IntendedMeans intendedMeans) { stack.add(intendedMeans); }

    public void pruneStack(int horizonCycle) {
        List<IntendedMeans> all = stack.snapshot();
        List<IntendedMeans> kept = new ArrayList<>(all.size());
        for (IntendedMeans im : all)
            if (im.getEnd() == null || im.getEnd().cycle() >= horizonCycle) kept.add(im);
        if (kept.size() != all.size()) stack.replaceAll(kept);
    }

    public List<IntendedMeans> stackAt(long sequenceEnd) {
        List<IntendedMeans> r = new ArrayList<>();
        for (IntendedMeans im : stack.snapshot())
            if (im.getStart().sequence() <= sequenceEnd && (im.getEnd() == null || im.getEnd().sequence() > sequenceEnd))
                r.add(im);
        return r;
    }

    public static State stateOf(Place place, Boolean suspended, Entity<?> waitingOn) {
        if (Boolean.TRUE.equals(suspended)) return State.SUSPENDED;
        if (waitingOn instanceof Action) return State.WAITING_ACTION;
        if (waitingOn instanceof Event) return State.WAITING_EVENT;
        if (place == Place.SELECTED_INTENTION) return State.SELECTED;
        if (place == Place.PENDING_INTENTIONS) return State.PENDING;
        return State.QUEUED;
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        Map<String, Timeline<?>> m = new LinkedHashMap<>();
        m.put("place", place);
        m.put("suspended", suspended);
        m.put("waitingOn", waitingOn);
        return m;
    }

    @Override public String typeName() { return "intention"; }

    @Override
    public String label() {
        if (getStart().cause() != null && getStart().cause().by() instanceof Event e)
            return e.getTrigger().before(getStart().sequence() + 1).text();
        List<IntendedMeans> s = stack.snapshot();
        return s.isEmpty() ? "#" + getId() : s.get(0).label();
    }
}
