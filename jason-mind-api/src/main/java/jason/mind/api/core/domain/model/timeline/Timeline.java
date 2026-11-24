package jason.mind.api.core.domain.model.timeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Timeline of a single value, stored as the ordered list of its changes.
 * Consecutive equal values are not recorded and sequences never decrease.
 *
 * @param <T> type of the value
 * @author Gabriel Lima
 */
public class Timeline<T> {

    /** Ordered value changes. */
    private final AppendOnlyList<TimelineEntry<T>> entries = new AppendOnlyList<>();

    /**
     * Records a new value, ignoring it when it equals the current one.
     *
     * @param occurrence occurrence that sets the value
     * @param value      new value
     * @return true if the value was recorded
     * @throws NullPointerException     if the occurrence is null
     * @throws IllegalArgumentException if the occurrence sequence precedes the last recorded one
     */
    public boolean put(Occurrence occurrence, T value) {
        Objects.requireNonNull(occurrence, "occurrence");
        TimelineEntry<T> last = this.entries.last();
        if (last != null) {
            if (Objects.equals(last.value(), value)) {
                return false;
            }
            if (occurrence.sequence() < last.occurrence().sequence()) {
                throw new IllegalArgumentException("sequence out of order in the Timeline");
            }
        }
        this.entries.add(new TimelineEntry<>(occurrence, value));
        return true;
    }

    /**
     * Returns a snapshot of the recorded entries.
     *
     * @return entries in recording order
     */
    public List<TimelineEntry<T>> entries() {
        return this.entries.snapshot();
    }

    /**
     * Returns the value at the end of a cycle.
     *
     * @param cycle cycle observed
     * @return last value set up to the cycle, or null
     */
    public T at(int cycle) {
        TimelineEntry<T> e = this.entryAt(cycle);
        return e == null ? null : e.value();
    }

    /**
     * Returns the entry in effect at the end of a cycle.
     *
     * @param cycle cycle observed
     * @return last entry set up to the cycle, or null
     */
    public TimelineEntry<T> entryAt(int cycle) {
        List<TimelineEntry<T>> list = this.entries.snapshot();
        int n = TimelineEntry.prefixLength(list, e -> e.occurrence().cycle() <= cycle);
        return n == 0 ? null : list.get(n - 1);
    }

    /**
     * Returns the value at a moment.
     *
     * @param moment moment observed
     * @return last value set up to the moment, inclusive, or null
     */
    public T at(Moment moment) {
        List<TimelineEntry<T>> list = this.entries.snapshot();
        int n = TimelineEntry.prefixLength(list, e -> e.occurrence().moment().compareTo(moment) <= 0);
        return n == 0 ? null : list.get(n - 1).value();
    }

    /**
     * Returns the value before a sequence.
     *
     * @param sequenceExclusive first sequence not considered
     * @return last value set with a lower sequence, or null
     */
    public T before(long sequenceExclusive) {
        List<TimelineEntry<T>> list = this.entries.snapshot();
        int n = TimelineEntry.prefixLength(list, e -> e.occurrence().sequence() < sequenceExclusive);
        return n == 0 ? null : list.get(n - 1).value();
    }

    /**
     * Returns the moment of the entry in effect at the end of a cycle.
     *
     * @param cycle cycle observed
     * @return moment of that entry, or null
     */
    public Moment momentAt(int cycle) {
        TimelineEntry<T> e = this.entryAt(cycle);
        return e == null ? null : e.occurrence().moment();
    }

    /**
     * Returns the moment of the last change.
     *
     * @return moment of the last entry, or null when empty
     */
    public Moment lastMoment() {
        TimelineEntry<T> e = this.entries.last();
        return e == null ? null : e.occurrence().moment();
    }

    /**
     * Returns the current value.
     *
     * @return value of the last entry, or null when empty
     */
    public T current() {
        TimelineEntry<T> e = this.entries.last();
        return e == null ? null : e.value();
    }

    /**
     * Drops the entries before the horizon cycle, keeping the last of them as the value in effect.
     *
     * @param horizonCycle first cycle whose entries are all kept
     */
    public void pruneBefore(int horizonCycle) {
        List<TimelineEntry<T>> list = this.entries.snapshot();
        int firstKept = TimelineEntry.prefixLength(list, e -> e.occurrence().cycle() < horizonCycle) - 1;
        if (firstKept > 0) {
            this.entries.replaceAll(new ArrayList<>(list.subList(firstKept, list.size())));
        }
    }

    /**
     * Returns the entries recorded from a cycle on.
     *
     * @param cycle first cycle observed
     * @return entries whose cycle is not before the given one, in recording order
     */
    public List<TimelineEntry<T>> entriesSince(int cycle) {
        List<TimelineEntry<T>> list = this.entries.snapshot();
        return list.subList(TimelineEntry.prefixLength(list, e -> e.occurrence().cycle() < cycle), list.size());
    }

    /**
     * Tells whether no value was recorded.
     *
     * @return true if the timeline is empty
     */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }
}
