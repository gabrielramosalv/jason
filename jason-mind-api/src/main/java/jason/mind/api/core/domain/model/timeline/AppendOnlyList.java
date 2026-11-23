package jason.mind.api.core.domain.model.timeline;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;

public final class AppendOnlyList<T> {

    private static final class State {
        final Object[] items;
        final int size;
        State(Object[] items, int size) { this.items = items; this.size = size; }
    }

    private volatile State state = new State(new Object[2], 0);

    public void add(T item) {
        State s = state;
        Object[] items = s.items;
        if (s.size == items.length)
            items = Arrays.copyOf(items, Math.max(4, items.length * 2));
        items[s.size] = item;
        state = new State(items, s.size + 1);
    }

    public void replaceAll(List<T> items) {
        Object[] arr = items.toArray(new Object[Math.max(2, items.size())]);
        state = new State(arr, items.size());
    }

    public int size() {
        return state.size;
    }

    public boolean isEmpty() {
        return state.size == 0;
    }

    @SuppressWarnings("unchecked")
    public T last() {
        State s = state;
        return s.size == 0 ? null : (T) s.items[s.size - 1];
    }

    public List<T> snapshot() {
        State s = state;
        return new Snapshot<>(s.items, s.size);
    }

    private static final class Snapshot<T> extends AbstractList<T> implements RandomAccess {
        private final Object[] items;
        private final int size;
        Snapshot(Object[] items, int size) { this.items = items; this.size = size; }
        @SuppressWarnings("unchecked")
        @Override public T get(int i) {
            if (i < 0 || i >= size) throw new IndexOutOfBoundsException(i);
            return (T) items[i];
        }
        @Override public int size() { return size; }
    }
}
