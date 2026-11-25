package jason.mind.api.core.domain.service.reasoning;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.TimelineEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Domain service that rebuilds the event and intention queues as they were at a given sequence.
 *
 * @author Gabriel Lima
 */
public final class QueueDomainService {

    /**
     * Tells whether an event had a goal state trigger at a sequence.
     *
     * @param e   event to inspect
     * @param cut sequence observed
     * @return true if the trigger operator was goal state
     */
    private static boolean isMeta(Event e, long cut) {
        Trigger t = e.getTrigger().before(cut + 1);
        return t != null && t.operator() == Trigger.Operator.GOAL_STATE;
    }

    /**
     * Tells whether a life span contains a sequence.
     *
     * @param start start sequence
     * @param end   end sequence, or null while alive
     * @param cut   sequence observed
     * @return true if started at or before the sequence and not ended at it
     */
    private static boolean aliveAt(long start, Long end, long cut) {
        return start <= cut && (end == null || end > cut);
    }

    /**
     * Finds the sequence of the last entry with a value up to a sequence.
     *
     * @param entries timeline entries to scan
     * @param value   value searched, compared by identity
     * @param cut     last sequence scanned
     * @param <T>     type of the value
     * @return sequence of the last matching entry, or {@link Long#MAX_VALUE} when none matches
     */
    private static <T> long lastEntrySequence(List<TimelineEntry<T>> entries, T value, long cut) {
        long r = Long.MAX_VALUE;
        for (TimelineEntry<T> e : entries) {
            if (e.occurrence().sequence() > cut) {
                break;
            }
            if (e.value() == value) {
                r = e.occurrence().sequence();
            }
        }
        return r;
    }

    /**
     * Rebuilds the event queue at a sequence. Atomic events come first, then goal state events, then the others,
     * each group ordered by when the event last entered the queue.
     *
     * @param events      events to consider
     * @param sequenceCut sequence at which the queue is observed
     * @return events alive and queued at the sequence, in queue order
     */
    public List<Event> eventQueue(List<Event> events, long sequenceCut) {
        List<Event> queue = new ArrayList<>();
        for (Event e : events) {
            if (!aliveAt(e.getStart().sequence(), e.getEnd() == null ? null : e.getEnd().sequence(), sequenceCut)) {
                continue;
            }
            if (e.getPlace().before(sequenceCut + 1) == Event.Place.QUEUE) {
                queue.add(e);
            }
        }
        queue.sort(Comparator.comparingInt((Event e) -> e.isAtomic() ? 0 : isMeta(e, sequenceCut) ? 1 : 2)
                .thenComparingLong(e -> lastEntrySequence(e.getPlace().entries(), Event.Place.QUEUE, sequenceCut)));
        return queue;
    }

    /**
     * Rebuilds the mailbox of an agent at a sequence, in the order the messages arrived.
     *
     * @param messages    messages to consider
     * @param agent       receiving agent
     * @param sequenceCut sequence at which the mailbox is observed
     * @return messages that had arrived at the agent and were not consumed at the sequence, oldest first
     */
    public List<Message> mailbox(List<Message> messages, String agent, long sequenceCut) {
        List<Message> mailbox = new ArrayList<>();
        for (Message m : messages) {
            if (!m.getReceiver().equals(agent) || m.getArrival() == null) {
                continue;
            }
            if (aliveAt(m.getArrival().sequence(), m.getEnd() == null ? null : m.getEnd().sequence(), sequenceCut)) {
                mailbox.add(m);
            }
        }
        mailbox.sort(Comparator.comparingLong((Message m) -> m.getArrival().sequence()));
        return mailbox;
    }

    /**
     * Rebuilds the intention queue at a sequence, ordered by when each intention last became running.
     *
     * @param intentions  intentions to consider
     * @param sequenceCut sequence at which the queue is observed
     * @return intentions alive, running and not waiting at the sequence, in queue order
     */
    public List<Intention> intentionQueue(List<Intention> intentions, long sequenceCut) {
        List<Intention> queue = new ArrayList<>();
        for (Intention i : intentions) {
            if (!aliveAt(i.getStart().sequence(), i.getEnd() == null ? null : i.getEnd().sequence(), sequenceCut)) {
                continue;
            }
            if (i.getPlace().before(sequenceCut + 1) == Intention.Place.RUNNING && i.getWaitingOn().before(
                    sequenceCut + 1) == null) {
                queue.add(i);
            }
        }
        queue.sort(Comparator.comparingLong(
                i -> lastEntrySequence(i.getPlace().entries(), Intention.Place.RUNNING, sequenceCut)));
        return queue;
    }
}
