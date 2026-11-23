package jason.mind.api.core.domain.model.timeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Timeline<T> {

    private final AppendOnlyList<TimelineEntry<T>> entries = new AppendOnlyList<>();

    public boolean put(Occurrence occurrence, T value) {
        Objects.requireNonNull(occurrence, "occurrence");
        TimelineEntry<T> last = entries.last();
        if (last != null) {
            if (Objects.equals(last.value(), value)) return false;
            if (occurrence.sequence() < last.occurrence().sequence())
                throw new IllegalArgumentException("sequence out of order in the Timeline [INV-037]");
        }
        entries.add(new TimelineEntry<>(occurrence, value));
        return true;
    }

    public List<TimelineEntry<T>> entries() {
        return entries.snapshot();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public T at(int cycle) {
        TimelineEntry<T> e = entryAt(cycle);
        return e == null ? null : e.value();
    }

    public TimelineEntry<T> entryAt(int cycle) {
        List<TimelineEntry<T>> list = entries.snapshot();
        TimelineEntry<T> found = null;
        for (TimelineEntry<T> e : list) {
            if (e.occurrence().cycle() <= cycle) found = e;
            else break;
        }
        return found;
    }

    public T at(Moment moment) {
        List<TimelineEntry<T>> list = entries.snapshot();
        T found = null;
        for (TimelineEntry<T> e : list) {
            if (e.occurrence().moment().compareTo(moment) <= 0) found = e.value();
            else break;
        }
        return found;
    }

    public T before(long sequenceExclusive) {
        List<TimelineEntry<T>> list = entries.snapshot();
        T found = null;
        for (TimelineEntry<T> e : list) {
            if (e.occurrence().sequence() < sequenceExclusive) found = e.value();
            else break;
        }
        return found;
    }

    public Moment momentAt(int cycle) {
        TimelineEntry<T> e = entryAt(cycle);
        return e == null ? null : e.occurrence().moment();
    }

    public Moment lastMoment() {
        TimelineEntry<T> e = entries.last();
        return e == null ? null : e.occurrence().moment();
    }

    public T current() {
        TimelineEntry<T> e = entries.last();
        return e == null ? null : e.value();
    }

    public void pruneBefore(int horizonCycle) {
        List<TimelineEntry<T>> list = entries.snapshot();
        int firstKept = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).occurrence().cycle() < horizonCycle) firstKept = i;
            else break;
        }
        if (firstKept > 0)
            entries.replaceAll(new ArrayList<>(list.subList(firstKept, list.size())));
    }
}
