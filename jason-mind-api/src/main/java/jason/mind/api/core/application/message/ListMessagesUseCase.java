package jason.mind.api.core.application.message;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.ports.in.message.ListMessagesIn;
import jason.mind.api.core.ports.in.message.ListMessagesPort;
import jason.mind.api.core.ports.in.message.MessagesOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Use case that lists the messages exchanged between agents page by page.
 * Each message contributes up to three points to a stream (sent, arrival and end), keyed by the closing
 * sequence of the cycle where the point happened; pages are cut on that stream and the cursors are the
 * formatted keys of the first and last points consumed.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class ListMessagesUseCase implements ListMessagesPort {

    /** Largest page a client may ask for; bounds the size of one response. */
    public static final int MAX_LIMIT = 1000;

    /** Read-only access to the messages and cycle records. */
    private final HistoryQueryRepositoryPort historyQuery;

    /**
     * Tells whether a message passes the agent, sender and receiver filters of the query.
     *
     * @param m  message being checked
     * @param in query with the filters; empty filters accept everything
     * @return {@code true} if the message passes every filter
     */
    private static boolean matches(Message m, ListMessagesIn in) {
        String s = m.getSender();
        String r = m.getReceiver();
        if (in.agents() != null && !in.agents().isEmpty() && !in.agents().contains(s) && !in.agents().contains(r)) {
            return false;
        }
        if (in.senders() != null && !in.senders().isEmpty() && !in.senders().contains(s)) {
            return false;
        }
        return in.receivers() == null || in.receivers().isEmpty() || in.receivers().contains(r);
    }

    /**
     * Builds the output of a tracked message, deriving its status from the visible points: "failed" when
     * it ended without arriving, "read" when it ended after arriving, "arrived" or "inFlight" otherwise.
     *
     * @param t tracked message
     * @return the message output
     */
    private static MessagesOut.MessageOut toOut(Track t) {
        Message m = t.message;
        String status;
        if (t.end != null && t.arrival == null) {
            status = "failed";
        } else if (t.end != null) {
            status = "read";
        } else if (t.arrival != null) {
            status = "arrived";
        } else {
            status = "inFlight";
        }
        return new MessagesOut.MessageOut(EntityIds.format(m), m.getSender(), m.getReceiver(),
                m.getPerformative().name(), m.getContent(), status,
                m.getInReplyTo() == null ? null : EntityIds.format(m.getInReplyTo()), resolvePoint(t.sent), resolvePoint(t.arrival),
                t.end != null && t.arrival != null ? resolvePoint(t.end) : null);
    }

    /**
     * Builds the output of a point.
     *
     * @param p point to describe, possibly {@code null}
     * @return the cycle and closing time of the point, or {@code null} when the point is {@code null}
     */
    private static MessagesOut.PointOut resolvePoint(Point p) {
        return p == null ? null : new MessagesOut.PointOut(p.occurrence().cycle(), p.cycle().timeEndMilliseconds());
    }

    /**
     * Counts the messages sent and received by each agent among the tracked messages.
     *
     * @param tracks tracked messages
     * @return the counts per agent, sorted by agent name
     */
    private static List<MessagesOut.CountOut> countMessages(List<Track> tracks) {
        Map<String, int[]> counts = new TreeMap<>();
        for (Track t : tracks) {
            if (t.sent != null) {
                counts.computeIfAbsent(t.message.getSender(), k -> new int[2])[0]++;
            }
            if (t.arrival != null) {
                counts.computeIfAbsent(t.message.getReceiver(), k -> new int[2])[1]++;
            }
        }
        List<MessagesOut.CountOut> r = new ArrayList<>();
        counts.forEach((a, c) -> r.add(new MessagesOut.CountOut(a, c[0], c[1])));
        r.sort(Comparator.comparing(MessagesOut.CountOut::agent));
        return r;
    }

    /**
     * Formats a stream key as a cursor in the form {@code sequenceEnd.agent.cycle.sequence}.
     *
     * @param k key of the point
     * @param t tracked message that owns the point
     * @return the cursor text
     */
    private static String format(Key k, Track t) {
        return k.sequenceEnd() + "." + findAgentOf(k, t) + "." + findCycleOf(k, t) + "." + k.sequence();
    }

    /**
     * Finds the agent of the point of a tracked message with a given key.
     *
     * @param k key of the point
     * @param t tracked message that owns the point
     * @return the agent of the point, or an empty string when no point has the key
     */
    private static String findAgentOf(Key k, Track t) {
        for (Point p : new Point[]{t.sent, t.arrival, t.end}) {
            if (p != null && p.key().equals(k)) {
                return p.occurrence().agent();
            }
        }
        return "";
    }

    /**
     * Finds the cycle of the point of a tracked message with a given key.
     *
     * @param k key of the point
     * @param t tracked message that owns the point
     * @return the cycle of the point, or zero when no point has the key
     */
    private static int findCycleOf(Key k, Track t) {
        for (Point p : new Point[]{t.sent, t.arrival, t.end}) {
            if (p != null && p.key().equals(k)) {
                return p.occurrence().cycle();
            }
        }
        return 0;
    }

    /**
     * Parses a cursor into a stream key, reading its first and last dot-separated parts.
     *
     * @param cursor cursor text
     * @return the stream key
     * @throws InvalidQueryException if the cursor is malformed
     */
    private static Key parse(String cursor) {
        String[] parts = cursor.split("\\.");
        try {
            if (parts.length < 2) {
                throw new NumberFormatException();
            }
            return new Key(Long.parseLong(parts[0]), Long.parseLong(parts[parts.length - 1]));
        } catch (NumberFormatException e) {
            throw new InvalidQueryException("invalid cursor: " + cursor);
        }
    }

    /**
     * Builds the stream point of an occurrence when it belongs to a closed cycle not after the limit.
     *
     * @param o     occurrence of the message, possibly {@code null}
     * @param until highest cycle closing sequence accepted
     * @return the point, or {@code null} when the occurrence is missing or not visible
     */
    private Point buildPoint(Occurrence o, long until) {
        if (o == null) {
            return null;
        }
        CycleRecord c = this.historyQuery.cycle(o.agent(), o.cycle()).orElse(null);
        if (c == null || o.sequence() > c.sequenceEnd() || c.sequenceEnd() > until) {
            return null;
        }
        return new Point(new Key(c.sequenceEnd(), o.sequence()), o, c);
    }

    @Override
    public MessagesOut execute(ListMessagesIn in) {
        if (in.limit() == null || in.limit() < 1) {
            throw new InvalidQueryException("limit is required and must be positive");
        }
        if (in.limit() > MAX_LIMIT) {
            throw new InvalidQueryException("limit must be at most " + MAX_LIMIT);
        }
        if (in.after() != null && in.before() != null) {
            throw new InvalidQueryException("after and before are exclusive");
        }
        Key after = in.after() == null ? null : parse(in.after());
        Key before = in.before() == null ? null : parse(in.before());
        long until = in.until() == null ? Long.MAX_VALUE : in.until();

        TreeMap<Key, Track> stream = new TreeMap<>();
        List<Track> tracks = new ArrayList<>();
        for (Message m : this.historyQuery.messages()) {
            if (!matches(m, in)) {
                continue;
            }
            Track t = new Track(m);
            t.sent = this.buildPoint(m.getStart(), until);
            t.arrival = this.buildPoint(m.getArrival(), until);
            t.end = this.buildPoint(m.getEnd(), until);
            if (t.sent == null && t.arrival == null && t.end == null) {
                continue;
            }
            tracks.add(t);
            for (Point p : new Point[]{t.sent, t.arrival, t.end}) {
                if (p != null) {
                    stream.put(p.key(), t);
                }
            }
        }

        List<Map.Entry<Key, Track>> consumed = new ArrayList<>();
        Map<Track, Boolean> page = new LinkedHashMap<>();
        if (after != null) {
            for (Map.Entry<Key, Track> e : stream.tailMap(after, false).entrySet()) {
                if (!page.containsKey(e.getValue()) && page.size() == in.limit()) {
                    break;
                }
                page.put(e.getValue(), true);
                consumed.add(e);
            }
        } else {
            Map<Key, Track> source = before == null ? stream.descendingMap() : stream.headMap(before, false)
                    .descendingMap();
            for (Map.Entry<Key, Track> e : source.entrySet()) {
                if (!page.containsKey(e.getValue()) && page.size() == in.limit()) {
                    break;
                }
                page.put(e.getValue(), true);
                consumed.add(e);
            }
            consumed.sort(Map.Entry.comparingByKey());
        }
        List<Track> ordered = new ArrayList<>();
        for (Map.Entry<Key, Track> e : consumed) {
            if (!ordered.contains(e.getValue())) {
                ordered.add(e.getValue());
            }
        }
        List<MessagesOut.MessageOut> messages = new ArrayList<>();
        for (Track t : ordered) {
            messages.add(toOut(t));
        }

        String previous = consumed.isEmpty() ? null : format(consumed.get(0).getKey(), consumed.get(0).getValue());
        String next;
        if (consumed.isEmpty()) {
            next = after == null ? null : in.after();
        } else {
            Map.Entry<Key, Track> last = consumed.get(consumed.size() - 1);
            next = format(last.getKey(), last.getValue());
        }
        return new MessagesOut(messages, countMessages(tracks), previous, next);
    }

    /**
     * Position of a point in the message stream, ordered by cycle closing sequence and then by occurrence sequence.
     *
     * @param sequenceEnd closing sequence of the cycle where the point happened
     * @param sequence    sequence of the occurrence of the point
     */
    private record Key(long sequenceEnd, long sequence) implements Comparable<Key> {

        @Override
        public int compareTo(Key o) {
            int c = Long.compare(this.sequenceEnd, o.sequenceEnd);
            return c != 0 ? c : Long.compare(this.sequence, o.sequence);
        }
    }

    /**
     * Point in the life of a message that is visible in a closed cycle.
     *
     * @param key        position of the point in the stream
     * @param occurrence occurrence of the point
     * @param cycle      closed cycle where the point happened
     */
    private record Point(Key key, Occurrence occurrence, CycleRecord cycle) {

    }

    /**
     * Tracking of a message with its visible points.
     * Points that are not visible stay {@code null}.
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class Track {

        /** Message being tracked. */
        final Message message;

        /** Point where the message was sent. */
        Point sent;

        /** Point where the message arrived at its receiver. */
        Point arrival;

        /** Point where the message ended. */
        Point end;
    }
}
