package jason.mind.api.core.domain.model.timeline;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Timeline of a set, stored as the ordered list of additions and removals.
 * The contents at any point are obtained by folding the operations up to that point.
 *
 * @param <T> type of the elements
 * @author Gabriel Lima
 */
public class CollectionTimeline<T> {

    /** Ordered additions and removals. */
    private final AppendOnlyList<TimelineEntry<CollectionChange<T>>> operations = new AppendOnlyList<>();

    /**
     * Records the addition of an element.
     *
     * @param element    element added
     * @param occurrence occurrence of the addition
     * @throws IllegalStateException if the element is already present
     */
    public void add(T element, Occurrence occurrence) {
        if (this.current().contains(element)) {
            throw new IllegalStateException("ADDED of an element already present: " + element);
        }
        this.operations.add(
                new TimelineEntry<>(occurrence, new CollectionChange<>(element, CollectionChange.Operation.ADDED)));
    }

    /**
     * Records the removal of an element.
     *
     * @param element    element removed
     * @param occurrence occurrence of the removal
     * @throws IllegalStateException if the element is absent
     */
    public void remove(T element, Occurrence occurrence) {
        if (!this.current().contains(element)) {
            throw new IllegalStateException("REMOVED of an absent element: " + element);
        }
        this.operations.add(
                new TimelineEntry<>(occurrence, new CollectionChange<>(element, CollectionChange.Operation.REMOVED)));
    }

    /**
     * Returns a snapshot of the recorded operations.
     *
     * @return operations in recording order
     */
    public List<TimelineEntry<CollectionChange<T>>> operations() {
        return this.operations.snapshot();
    }

    /**
     * Returns the current contents.
     *
     * @return elements after all operations
     */
    public Set<T> current() {
        return this.foldOperations(Integer.MAX_VALUE);
    }

    /**
     * Returns the contents at the end of a cycle.
     *
     * @param cycle cycle observed
     * @return elements after the operations up to the cycle, inclusive
     */
    public Set<T> elementsAt(int cycle) {
        return this.foldOperations(cycle);
    }

    /**
     * Returns the contents at a moment.
     *
     * @param moment moment observed
     * @return elements after the operations up to the moment, inclusive
     */
    public Set<T> elementsAt(Moment moment) {
        List<TimelineEntry<CollectionChange<T>>> list = this.operations.snapshot();
        return this.fold(list, TimelineEntry.prefixLength(list, e -> e.occurrence().moment().compareTo(moment) <= 0));
    }

    /**
     * Returns the contents before a sequence.
     *
     * @param sequenceExclusive first sequence not applied
     * @return elements after the operations with a lower sequence
     */
    public Set<T> elementsBefore(long sequenceExclusive) {
        List<TimelineEntry<CollectionChange<T>>> list = this.operations.snapshot();
        return this.fold(list, TimelineEntry.prefixLength(list, e -> e.occurrence().sequence() < sequenceExclusive));
    }

    /**
     * Returns the operations recorded from a cycle on.
     *
     * @param cycle first cycle observed
     * @return operations whose cycle is not before the given one, in recording order
     */
    public List<TimelineEntry<CollectionChange<T>>> operationsSince(int cycle) {
        List<TimelineEntry<CollectionChange<T>>> list = this.operations.snapshot();
        return list.subList(TimelineEntry.prefixLength(list, e -> e.occurrence().cycle() < cycle), list.size());
    }

    /**
     * Collapses the operations before the horizon cycle into additions of the elements present at that point.
     *
     * @param horizonCycle first cycle whose operations are kept as they are
     */
    public void pruneBefore(int horizonCycle) {
        List<TimelineEntry<CollectionChange<T>>> list = this.operations.snapshot();
        int firstKept = TimelineEntry.prefixLength(list, e -> e.occurrence().cycle() < horizonCycle);
        if (firstKept == 0) {
            return;
        }
        Set<T> folded = new LinkedHashSet<>();
        for (int i = 0; i < firstKept; i++) {
            this.apply(folded, list.get(i).value());
        }
        List<TimelineEntry<CollectionChange<T>>> kept = new java.util.ArrayList<>();
        Occurrence at = list.get(firstKept - 1).occurrence();
        for (T element : folded) {
            kept.add(new TimelineEntry<>(at, new CollectionChange<>(element, CollectionChange.Operation.ADDED)));
        }
        kept.addAll(list.subList(firstKept, list.size()));
        this.operations.replaceAll(kept);
    }

    /**
     * Folds the operations up to a cycle.
     *
     * @param cycle last cycle applied
     * @return elements after the folded operations
     */
    private Set<T> foldOperations(int cycle) {
        List<TimelineEntry<CollectionChange<T>>> list = this.operations.snapshot();
        return this.fold(list, TimelineEntry.prefixLength(list, e -> e.occurrence().cycle() <= cycle));
    }

    /**
     * Folds the first operations of a list.
     *
     * @param list  operations in recording order
     * @param count number of leading operations to apply
     * @return elements after the folded operations
     */
    private Set<T> fold(List<TimelineEntry<CollectionChange<T>>> list, int count) {
        Set<T> r = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            this.apply(r, list.get(i).value());
        }
        return r;
    }

    /**
     * Applies a change to a set.
     *
     * @param set set being folded
     * @param c   change to apply
     */
    private void apply(Set<T> set, CollectionChange<T> c) {
        if (c.operation() == CollectionChange.Operation.ADDED) {
            set.add(c.element());
        } else {
            set.remove(c.element());
        }
    }
}
