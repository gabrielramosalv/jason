package jason.mind.api.infrastructure.adapters.out.persistence.memory.history;

import jason.mind.api.core.domain.model.entities.Binding;
import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.timeline.CollectionTimeline;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.ports.out.history.Footprint;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.LongConsumer;

/**
 * In-memory store of the history of a single agent.
 * Keeps the entities and the closed cycles of the agent; the occurrences, plan selections and deed executions are
 * read back from the entities themselves. Prunes everything older than the retention horizon.
 *
 * @author Gabriel Lima
 */
final class AgentStore {

    /** Entities owned by the agent, by id. */
    final Map<Long, Entity> entities = new ConcurrentHashMap<>();

    /** Messages received by the agent and owned by another agent, by id. */
    final Map<Long, Message> incoming = new ConcurrentHashMap<>();

    /** Closed cycles, by cycle number. */
    final ConcurrentSkipListMap<Integer, CycleRecord> cycles = new ConcurrentSkipListMap<>();

    /** Oldest cycle to keep asked for by a manual prune and not applied yet; 0 when nothing is pending. */
    final java.util.concurrent.atomic.AtomicInteger pendingPrune = new java.util.concurrent.atomic.AtomicInteger();

    /** Last count of what the store holds; renewed after {@link #FOOTPRINT_TTL_NANOS}. */
    private volatile Footprint footprint = new Footprint(0, 0, 0);

    /** {@link System#nanoTime()} of the last count, or 0 when none was made. */
    private volatile long footprintAt;

    /** Ids of the entities removed by pruning; guarded by its own monitor. */
    private final BitSet pruned = new BitSet();

    /** Agent whose history is stored. */
    volatile Agent agent;

    /** Last closed cycle, or {@code null} when no cycle was closed yet. */
    volatile CycleRecord lastClosed;

    /** Oldest retained cycle; everything before it is pruned. */
    volatile int horizon = Integer.MIN_VALUE;

    /** Horizon of the last full scan for dead entities; occurrences before it were dropped from the entities. */
    volatile int lastScanHorizon = Integer.MIN_VALUE;

    /** Called with the id of every message removed by pruning, so that indexes outside the history can forget it. */
    private final LongConsumer messagePruned;

    /**
     * Creates an empty store for an agent.
     *
     * @param agent         {@link #agent}
     * @param messagePruned {@link #messagePruned}
     */
    AgentStore(Agent agent, LongConsumer messagePruned) {
        this.agent = agent;
        this.messagePruned = messagePruned;
    }

    /** How long a count of the footprint is reused, in nanoseconds. */
    private static final long FOOTPRINT_TTL_NANOS = 5_000_000_000L;

    /**
     * Counts the entities, the timeline entries and the caused occurrences held, reusing a count made in the last few
     * seconds because walking every entity of a long history is not free.
     *
     * @return the count
     */
    Footprint footprint() {
        long now = System.nanoTime();
        if (this.footprintAt != 0 && now - this.footprintAt < FOOTPRINT_TTL_NANOS) {
            return this.footprint;
        }
        long entries = 0;
        long effects = 0;
        long count = 0;
        for (Entity e : this.entities.values()) {
            count++;
            entries += entriesOf(e);
            effects += e.getEffects().size();
        }
        for (Message m : this.incoming.values()) {
            if (this.entities.get(m.getId()) != m) {
                count++;
                entries += entriesOf(m);
                effects += m.getEffects().size();
            }
        }
        this.footprint = new Footprint(count, entries, effects);
        this.footprintAt = now;
        return this.footprint;
    }

    /**
     * Counts the entries of every timeline and collection timeline of an entity.
     *
     * @param e entity to count
     * @return number of entries
     */
    private static long entriesOf(Entity e) {
        long n = 0;
        for (Timeline<?> t : e.timelines().values()) {
            n += t.entries().size();
        }
        for (CollectionTimeline<?> c : e.collections().values()) {
            n += c.operations().size();
        }
        return n;
    }

    /**
     * Tells whether the store holds nothing: no entity, no incoming message and no closed cycle.
     *
     * @return {@code true} when pruning removed everything the agent had recorded
     */
    boolean isEmpty() {
        return this.entities.isEmpty() && this.incoming.isEmpty() && this.cycles.isEmpty();
    }

    /**
     * Drops the values of the timelines, collections, bindings, intention stack, plan selections and caused
     * occurrences of an entity older than the horizon.
     *
     * @param e     entity to trim
     * @param agent name of the agent whose caused occurrences are dropped
     * @param h     horizon of the trim
     */
    private static void pruneTimelines(Entity e, String agent, int h) {
        for (Timeline<?> t : e.timelines().values()) {
            t.pruneBefore(h);
        }
        for (CollectionTimeline<?> c : e.collections().values()) {
            c.pruneBefore(h);
        }
        if (e instanceof IntendedMeans im) {
            pruneBindings(im, h);
        }
        if (e instanceof Intention i) {
            i.pruneStack(h);
        }
        if (e instanceof Event ev) {
            ev.pruneSelectionsBefore(h);
        }
        e.pruneEffectsBefore(agent, h);
    }

    /**
     * Drops what a pruned entity still points to: the occurrences it caused in this agent and, for an event, its plan
     * selections. Without this, a pruned entity that is still reachable through the cause of a live one would keep
     * every entity it caused alive.
     *
     * @param e     entity removed by pruning
     * @param agent name of the agent whose caused occurrences are dropped
     */
    private static void releaseCaused(Entity e, String agent) {
        e.pruneEffectsBefore(agent, Integer.MAX_VALUE);
        if (e instanceof Event ev) {
            ev.pruneSelectionsBefore(Integer.MAX_VALUE);
        }
    }

    /**
     * Drops the bindings of an intended means older than the horizon, keeping the last value of each variable bound
     * before it.
     *
     * @param im intended means to trim
     * @param h  horizon of the trim
     */
    private static void pruneBindings(IntendedMeans im, int h) {
        List<Binding> all = im.getBindings();
        if (all.isEmpty() || all.get(0).moment().cycle() >= h) {
            return;
        }
        Map<String, Binding> lastBefore = new java.util.LinkedHashMap<>();
        List<Binding> after = new ArrayList<>();
        for (Binding b : all) {
            if (b.moment().cycle() < h) {
                lastBefore.remove(b.name());
                lastBefore.put(b.name(), b);
            } else {
                after.add(b);
            }
        }
        List<Binding> kept = new ArrayList<>(lastBefore.values());
        kept.addAll(after);
        im.replaceBindings(kept);
    }

    /**
     * Tells whether an entity was removed by pruning or ended before the horizon.
     *
     * @param id id of the entity
     * @return {@code true} when the entity is no longer within the retained history
     */
    boolean wasPruned(long id) {
        synchronized (this.pruned) {
            if (id <= Integer.MAX_VALUE && this.pruned.get((int) id)) {
                return true;
            }
        }
        Entity e = this.entities.get(id);
        if (e == null) {
            e = this.incoming.get(id);
        }
        return e != null && deadBeforeHorizon(e);
    }

    /**
     * Tells whether an entity ended, from the point of view of this agent, before the horizon.
     *
     * @param e entity to check
     * @return {@code true} when the last own occurrence of the entity is older than the horizon
     */
    boolean deadBeforeHorizon(Entity e) {
        Occurrence end = lastOwnOccurrence(e);
        return end != null && end.cycle() < this.horizon;
    }

    /**
     * Moves the horizon forward and drops the cycles before it. The scan for dead
     * entities only runs when the horizon advanced by at least a tenth of the retained cycles since the last scan.
     *
     * @param newHorizon oldest cycle to retain; ignored when it is not after the current horizon
     */
    void prune(int newHorizon) {
        if (newHorizon <= this.horizon) {
            return;
        }
        this.horizon = newHorizon;
        this.cycles.headMap(newHorizon).clear();
        int retained = this.lastClosed == null ? 1 : Math.max(1, this.lastClosed.cycle() - newHorizon + 1);
        if (this.lastScanHorizon != Integer.MIN_VALUE && newHorizon - this.lastScanHorizon < Math.max(1,
                retained / 10)) {
            return;
        }
        this.lastScanHorizon = newHorizon;
        scan(newHorizon);
    }

    /**
     * Finds the occurrence that ends an entity for this agent. For a message, it is the send when this agent
     * sent it to another agent, or the end when the end happened in this agent.
     *
     * @param e entity to check
     * @return occurrence that ends the entity for this agent, or {@code null} when it has not ended for it
     */
    private Occurrence lastOwnOccurrence(Entity e) {
        if (e instanceof Message m) {
            Occurrence end = m.getEnd();
            if (end == null) {
                return null;
            }
            String name = this.agent.getName();
            if (m.getStart() != null && m.getStart().agent().equals(name) && !m.getReceiver().equals(name)) {
                return m.getStart();
            }
            return end.agent().equals(name) ? end : null;
        }
        return e.getEnd();
    }

    /**
     * Removes the entities that ended before the horizon, marks them as pruned and trims the timelines of the
     * remaining ones and the occurrences caused in this agent by the remaining incoming messages.
     *
     * @param h horizon of the scan
     */
    private void scan(int h) {
        String name = this.agent.getName();
        List<Long> dead = new ArrayList<>();
        for (Entity e : this.entities.values()) {
            if (deadBeforeHorizon(e)) {
                dead.add(e.getId());
                releaseCaused(e, name);
            } else {
                pruneTimelines(e, name, h);
            }
        }
        for (Message m : this.incoming.values()) {
            if (deadBeforeHorizon(m)) {
                dead.add(m.getId());
                releaseCaused(m, name);
            } else if (this.entities.get(m.getId()) != m) {
                m.pruneEffectsBefore(name, h);
            }
        }
        synchronized (this.pruned) {
            for (long id : dead) {
                if (id <= Integer.MAX_VALUE) {
                    this.pruned.set((int) id);
                }
            }
        }
        for (long id : dead) {
            Entity removed = this.entities.remove(id);
            Entity removedIncoming = this.incoming.remove(id);
            if (removed instanceof Message || removedIncoming instanceof Message) {
                this.messagePruned.accept(id);
            }
        }
    }
}
