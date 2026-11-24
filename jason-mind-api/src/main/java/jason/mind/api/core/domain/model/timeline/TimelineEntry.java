package jason.mind.api.core.domain.model.timeline;

import java.util.List;
import java.util.function.Predicate;

/**
 * Entry of a timeline: a value set by an occurrence.
 *
 * @param occurrence occurrence that set the value
 * @param value      value set
 * @param <T>        type of the value
 * @author Gabriel Lima
 */
public record TimelineEntry<T>(Occurrence occurrence, T value) {

    /**
     * Counts the leading entries of a list that satisfy a condition, by binary search. The entries are sorted by
     * sequence, so any condition that only holds up to some sequence, cycle or moment holds for a prefix of the list.
     *
     * @param list     entries sorted by sequence
     * @param inPrefix condition that holds for a prefix of the list and fails for the rest
     * @param <E>      type of the entries
     * @return length of the prefix that satisfies the condition
     */
    static <E extends TimelineEntry<?>> int prefixLength(List<E> list, Predicate<? super E> inPrefix) {
        int low = 0;
        int high = list.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (inPrefix.test(list.get(mid))) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
