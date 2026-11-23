package jason.mind.api.core.application.message;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.ports.in.message.ListMessagesIn;
import jason.mind.api.core.ports.in.message.ListMessagesPort;
import jason.mind.api.core.ports.in.message.MessagesOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

public class ListMessagesUseCase implements ListMessagesPort {

    private final HistoryQueryRepositoryPort historyQuery;

    public ListMessagesUseCase(HistoryQueryRepositoryPort historyQuery) {
        this.historyQuery = historyQuery;
    }

    private record Key(long sequenceEnd, long sequence) implements Comparable<Key> {
        public int compareTo(Key o) {
            int c = Long.compare(sequenceEnd, o.sequenceEnd);
            return c != 0 ? c : Long.compare(sequence, o.sequence);
        }
    }

    private record Point(Key key, Occurrence occurrence, CycleRecord cycle) {
    }

    private static final class Track {
        final Message message;
        Point sent;
        Point arrival;
        Point end;

        Track(Message message) {
            this.message = message;
        }
    }

    @Override
    public MessagesOut execute(ListMessagesIn in) {
        if (in.limit() == null || in.limit() < 1) throw new InvalidQueryException("limit is required and must be positive");
        if (in.after() != null && in.before() != null) throw new InvalidQueryException("after and before are exclusive");
        Key after = in.after() == null ? null : parse(in.after());
        Key before = in.before() == null ? null : parse(in.before());
        long until = in.until() == null ? Long.MAX_VALUE : in.until();

        TreeMap<Key, Track> stream = new TreeMap<>();
        List<Track> tracks = new ArrayList<>();
        for (Message m : historyQuery.messages()) {
            if (!matches(m, in)) continue;
            Track t = new Track(m);
            t.sent = point(m.getStart(), until);
            t.arrival = point(m.getArrival(), until);
            t.end = point(m.getEnd(), until);
            if (t.sent == null && t.arrival == null && t.end == null) continue;
            tracks.add(t);
            for (Point p : new Point[] { t.sent, t.arrival, t.end })
                if (p != null) stream.put(p.key(), t);
        }

        List<Map.Entry<Key, Track>> consumed = new ArrayList<>();
        Map<Track, Boolean> page = new LinkedHashMap<>();
        if (after != null) {
            for (Map.Entry<Key, Track> e : stream.tailMap(after, false).entrySet()) {
                if (!page.containsKey(e.getValue()) && page.size() == in.limit()) break;
                page.put(e.getValue(), true);
                consumed.add(e);
            }
        } else {
            Map<Key, Track> source = before == null ? stream.descendingMap() : stream.headMap(before, false).descendingMap();
            for (Map.Entry<Key, Track> e : source.entrySet()) {
                if (!page.containsKey(e.getValue()) && page.size() == in.limit()) break;
                page.put(e.getValue(), true);
                consumed.add(e);
            }
            consumed.sort(Map.Entry.comparingByKey());
        }
        List<Track> ordered = new ArrayList<>();
        for (Map.Entry<Key, Track> e : consumed) if (!ordered.contains(e.getValue())) ordered.add(e.getValue());
        List<MessagesOut.MessageOut> messages = new ArrayList<>();
        for (Track t : ordered) messages.add(out(t));

        String previous = consumed.isEmpty() ? null : format(consumed.get(0).getKey(), consumed.get(0).getValue());
        String next;
        if (consumed.isEmpty()) next = after == null ? null : in.after();
        else {
            Map.Entry<Key, Track> last = consumed.get(consumed.size() - 1);
            next = format(last.getKey(), last.getValue());
        }
        return new MessagesOut(messages, counts(tracks), previous, next);
    }

    private static boolean matches(Message m, ListMessagesIn in) {
        String s = m.getSender();
        String r = m.getReceiver();
        if (in.agents() != null && !in.agents().isEmpty() && !in.agents().contains(s) && !in.agents().contains(r)) return false;
        if (in.senders() != null && !in.senders().isEmpty() && !in.senders().contains(s)) return false;
        return in.receivers() == null || in.receivers().isEmpty() || in.receivers().contains(r);
    }

    private Point point(Occurrence o, long until) {
        if (o == null) return null;
        CycleRecord c = historyQuery.cycle(o.agent(), o.cycle()).orElse(null);
        if (c == null || o.sequence() > c.sequenceEnd() || c.sequenceEnd() > until) return null;
        return new Point(new Key(c.sequenceEnd(), o.sequence()), o, c);
    }

    private static MessagesOut.MessageOut out(Track t) {
        Message m = t.message;
        String status;
        if (t.end != null && t.arrival == null) status = "failed";
        else if (t.end != null) status = "read";
        else if (t.arrival != null) status = "arrived";
        else status = "inFlight";
        return new MessagesOut.MessageOut(EntityIds.format(m), m.getSender(), m.getReceiver(), m.getPerformative().name(),
                m.getContent(), status, m.getInReplyTo() == null ? null : EntityIds.format(m.getInReplyTo()), point(t.sent),
                point(t.arrival), t.end != null && t.arrival != null ? point(t.end) : null);
    }

    private static MessagesOut.PointOut point(Point p) {
        return p == null ? null : new MessagesOut.PointOut(p.occurrence().cycle(), p.cycle().timeEndMilliseconds());
    }

    private static List<MessagesOut.CountOut> counts(List<Track> tracks) {
        Map<String, int[]> counts = new TreeMap<>();
        for (Track t : tracks) {
            if (t.sent != null) counts.computeIfAbsent(t.message.getSender(), k -> new int[2])[0]++;
            if (t.arrival != null) counts.computeIfAbsent(t.message.getReceiver(), k -> new int[2])[1]++;
        }
        List<MessagesOut.CountOut> r = new ArrayList<>();
        counts.forEach((a, c) -> r.add(new MessagesOut.CountOut(a, c[0], c[1])));
        r.sort(Comparator.comparing(MessagesOut.CountOut::agent));
        return r;
    }

    private static String format(Key k, Track t) {
        return k.sequenceEnd() + "." + agentOf(k, t) + "." + cycleOf(k, t) + "." + k.sequence();
    }

    private static String agentOf(Key k, Track t) {
        for (Point p : new Point[] { t.sent, t.arrival, t.end })
            if (p != null && p.key().equals(k)) return p.occurrence().agent();
        return "";
    }

    private static int cycleOf(Key k, Track t) {
        for (Point p : new Point[] { t.sent, t.arrival, t.end })
            if (p != null && p.key().equals(k)) return p.occurrence().cycle();
        return 0;
    }

    private static Key parse(String cursor) {
        String[] parts = cursor.split("\\.");
        try {
            if (parts.length < 2) throw new NumberFormatException();
            return new Key(Long.parseLong(parts[0]), Long.parseLong(parts[parts.length - 1]));
        } catch (NumberFormatException e) {
            throw new InvalidQueryException("invalid cursor: " + cursor);
        }
    }
}
