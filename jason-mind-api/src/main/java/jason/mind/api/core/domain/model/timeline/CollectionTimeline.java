package jason.mind.api.core.domain.model.timeline;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CollectionTimeline<T> {

    private final AppendOnlyList<TimelineEntry<CollectionChange<T>>> operations = new AppendOnlyList<>();

    public void add(T element, Occurrence occurrence) {
        if (current().contains(element))
            throw new IllegalStateException("ADDED of an element already present [INV-031]: " + element);
        operations.add(new TimelineEntry<>(occurrence, new CollectionChange<>(element, CollectionChange.Operation.ADDED)));
    }

    public void remove(T element, Occurrence occurrence) {
        if (!current().contains(element))
            throw new IllegalStateException("REMOVED of an absent element [INV-031]: " + element);
        operations.add(new TimelineEntry<>(occurrence, new CollectionChange<>(element, CollectionChange.Operation.REMOVED)));
    }

    public List<TimelineEntry<CollectionChange<T>>> operations() {
        return operations.snapshot();
    }

    public Set<T> current() {
        return fold(Integer.MAX_VALUE);
    }

    public Set<T> elementsAt(int cycle) {
        return fold(cycle);
    }

    public Set<T> elementsAt(Moment moment) {
        Set<T> r = new LinkedHashSet<>();
        for (TimelineEntry<CollectionChange<T>> e : operations.snapshot()) {
            if (e.occurrence().moment().compareTo(moment) > 0) break;
            apply(r, e.value());
        }
        return r;
    }

    public Set<T> elementsBefore(long sequenceExclusive) {
        Set<T> r = new LinkedHashSet<>();
        for (TimelineEntry<CollectionChange<T>> e : operations.snapshot()) {
            if (e.occurrence().sequence() >= sequenceExclusive) break;
            apply(r, e.value());
        }
        return r;
    }

    public void pruneBefore(int horizonCycle) {
        List<TimelineEntry<CollectionChange<T>>> list = operations.snapshot();
        int firstKept = 0;
        while (firstKept < list.size() && list.get(firstKept).occurrence().cycle() < horizonCycle) firstKept++;
        if (firstKept == 0) return;
        Set<T> folded = new LinkedHashSet<>();
        for (int i = 0; i < firstKept; i++) apply(folded, list.get(i).value());
        List<TimelineEntry<CollectionChange<T>>> kept = new java.util.ArrayList<>();
        Occurrence at = list.get(firstKept - 1).occurrence();
        for (T element : folded) kept.add(new TimelineEntry<>(at, new CollectionChange<>(element, CollectionChange.Operation.ADDED)));
        kept.addAll(list.subList(firstKept, list.size()));
        operations.replaceAll(kept);
    }

    private Set<T> fold(int cycle) {
        Set<T> r = new LinkedHashSet<>();
        for (TimelineEntry<CollectionChange<T>> e : operations.snapshot()) {
            if (e.occurrence().cycle() > cycle) break;
            apply(r, e.value());
        }
        return r;
    }

    private void apply(Set<T> set, CollectionChange<T> c) {
        if (c.operation() == CollectionChange.Operation.ADDED) set.add(c.element());
        else set.remove(c.element());
    }
}
