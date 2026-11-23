package jason.mind.api.core.domain.service.reasoning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.TimelineEntry;

public final class QueueDomainService {

    public List<Event> eventQueue(List<Event> events, long sequenceCut) {
        List<Event> queue = new ArrayList<>();
        for (Event e : events) {
            if (!aliveAt(e.getStart().sequence(), e.getEnd() == null ? null : e.getEnd().sequence(), sequenceCut)) continue;
            if (e.getPlace().before(sequenceCut + 1) == Event.Place.QUEUE) queue.add(e);
        }
        queue.sort(Comparator.comparingInt((Event e) -> e.isAtomic() ? 0 : isMeta(e, sequenceCut) ? 1 : 2)
                .thenComparingLong(e -> lastEntrySequence(e.getPlace().entries(), Event.Place.QUEUE, sequenceCut)));
        return queue;
    }

    public List<Intention> intentionQueue(List<Intention> intentions, long sequenceCut) {
        List<Intention> queue = new ArrayList<>();
        for (Intention i : intentions) {
            if (!aliveAt(i.getStart().sequence(), i.getEnd() == null ? null : i.getEnd().sequence(), sequenceCut)) continue;
            if (i.getPlace().before(sequenceCut + 1) == Intention.Place.RUNNING && i.getWaitingOn().before(sequenceCut + 1) == null)
                queue.add(i);
        }
        queue.sort(Comparator.comparingLong(i -> lastEntrySequence(i.getPlace().entries(), Intention.Place.RUNNING, sequenceCut)));
        return queue;
    }

    private static boolean isMeta(Event e, long cut) {
        Trigger t = e.getTrigger().before(cut + 1);
        return t != null && t.operator() == Trigger.Operator.GOAL_STATE;
    }

    private static boolean aliveAt(long start, Long end, long cut) {
        return start <= cut && (end == null || end > cut);
    }

    private static <T> long lastEntrySequence(List<TimelineEntry<T>> entries, T value, long cut) {
        long r = Long.MAX_VALUE;
        for (TimelineEntry<T> e : entries) {
            if (e.occurrence().sequence() > cut) break;
            if (e.value() == value) r = e.occurrence().sequence();
        }
        return r;
    }
}
