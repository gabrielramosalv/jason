package jason.mind.api.core.domain.model.timeline;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;

/**
 * List that only grows at the end or is replaced as a whole.
 * Reads take immutable snapshots of a volatile state, so a single writer can append while readers iterate.
 *
 * @param <T> type of the elements
 * @author Gabriel Lima
 */
public final class AppendOnlyList<T> {

    /** Current state of the list. */
    private volatile State state = new State(new Object[2], 0);

    /**
     * Appends an element, growing the backing array when it is full.
     *
     * @param item element to append
     */
    public void add(T item) {
        State s = this.state;
        Object[] items = s.items;
        if (s.size == items.length) {
            items = Arrays.copyOf(items, Math.max(4, items.length * 2));
        }
        items[s.size] = item;
        this.state = new State(items, s.size + 1);
    }

    /**
     * Replaces all elements with the given ones.
     *
     * @param items new elements of the list
     */
    public void replaceAll(List<T> items) {
        Object[] arr = items.toArray(new Object[Math.max(2, items.size())]);
        this.state = new State(arr, items.size());
    }

    /**
     * Returns the number of elements.
     *
     * @return current size
     */
    public int size() {
        return this.state.size;
    }

    /**
     * Returns the last element.
     *
     * @return last element, or null when the list is empty
     */
    @SuppressWarnings("unchecked")
    public T last() {
        State s = this.state;
        return s.size == 0 ? null : (T) s.items[s.size - 1];
    }

    /**
     * Returns an immutable view of the current elements.
     *
     * @return snapshot unaffected by later appends
     */
    public List<T> snapshot() {
        State s = this.state;
        return new Snapshot<>(s.items, s.size);
    }

    /**
     * Tells whether the list is empty.
     *
     * @return true if there is no element
     */
    public boolean isEmpty() {
        return this.state.size == 0;
    }

    /**
     * Immutable state of the list: the backing array and how many of its slots are filled.
     *
     * @param items Backing array of the elements.
     * @param size  Number of filled slots.
     */
        private record State(Object[] items, int size) {

    }

    /**
     * Read-only view over a prefix of a backing array.
     *
     * @param <T> type of the elements
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class Snapshot<T> extends AbstractList<T> implements RandomAccess {

        /** Backing array of the elements. */
        private final Object[] items;

        /** Number of visible elements. */
        private final int size;

        @SuppressWarnings("unchecked")
        @Override
        public T get(int i) {
            if (i < 0 || i >= this.size) {
                throw new IndexOutOfBoundsException(i);
            }
            return (T) this.items[i];
        }

        @Override
        public int size() {
            return this.size;
        }
    }
}
