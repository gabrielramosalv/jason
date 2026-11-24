package jason.mind.api.core.domain.model.timeline;

import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base of every entity whose life is recorded in the history.
 * Keeps its identifier, start and end and the occurrences it caused, and exposes its timelines. Equality is by
 * identity.
 *
 * @author Gabriel Lima
 */
@Getter
public abstract class Entity {

    /** Unique identifier of the entity. */
    private final long id;

    /** Occurrence that started the entity. */
    private final Occurrence start;

    /** Occurrence that ended the entity, or null while alive. */
    private volatile Occurrence end;

    /** Occurrences caused by the entity, in recording order, or null until the first one. */
    private volatile AppendOnlyList<OccurrenceRecord> effects;

    /**
     * Creates the entity with its identifier and start occurrence.
     *
     * @param id    {@link #id}
     * @param start {@link #start}
     */
    protected Entity(long id, Occurrence start) {
        this.id = id;
        this.start = start;
    }

    /**
     * Records the end of the entity.
     *
     * @param end occurrence that ends the entity
     * @throws NullPointerException  if the end is null
     * @throws IllegalStateException if the entity already ended or the end precedes the start of its life
     */
    public void finish(Occurrence end) {
        Objects.requireNonNull(end, "end");
        if (this.end != null) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " " + this.id + " already ended");
        }
        Occurrence s = this.lifeStart();

        if (s != null && s.agent().equals(end.agent()) && end.sequence() < s.sequence()) {
            throw new IllegalStateException("end before start");
        }
        this.end = end;
    }

    /**
     * Appends an occurrence caused by the entity. Synchronized because a message may cause occurrences in its sender
     * and in its receiver.
     *
     * @param effect occurrence whose cause is this entity
     */
    public synchronized void addEffect(OccurrenceRecord effect) {
        AppendOnlyList<OccurrenceRecord> l = this.effects;
        if (l == null) {
            l = new AppendOnlyList<>();
            this.effects = l;
        }
        l.add(effect);
    }

    /**
     * Returns the occurrences caused by the entity.
     *
     * @return snapshot of the occurrences caused by the entity, in recording order
     */
    public List<OccurrenceRecord> getEffects() {
        AppendOnlyList<OccurrenceRecord> l = this.effects;
        return l == null ? List.of() : l.snapshot();
    }

    /**
     * Drops the occurrences caused by the entity in an agent before the horizon cycle of that agent.
     *
     * @param agent        name of the agent whose occurrences are dropped
     * @param horizonCycle first cycle whose occurrences are kept
     */
    public synchronized void pruneEffectsBefore(String agent, int horizonCycle) {
        AppendOnlyList<OccurrenceRecord> l = this.effects;
        if (l == null) {
            return;
        }
        List<OccurrenceRecord> all = l.snapshot();
        List<OccurrenceRecord> kept = new ArrayList<>(all.size());
        for (OccurrenceRecord r : all) {
            if (r.occurrence().cycle() >= horizonCycle || !r.occurrence().agent().equals(agent)) {
                kept.add(r);
            }
        }
        if (kept.size() != all.size()) {
            l.replaceAll(kept);
        }
    }

    /**
     * Tells whether the entity exists in a cycle of an agent.
     *
     * @param agentCycle agent and cycle observed
     * @return true if the entity started in that agent at or before the cycle and did not end before it
     */
    public boolean existsAt(AgentCycle agentCycle) {
        if (this.start == null || !this.start.agent().equals(agentCycle.agent())) {
            return false;
        }
        if (this.start.cycle() > agentCycle.cycle()) {
            return false;
        }
        Occurrence e = this.end;
        return e == null || e.cycle() >= agentCycle.cycle();
    }

    /**
     * Returns the collection timelines of the entity.
     *
     * @return collection timelines by name, empty by default
     */
    public Map<String, CollectionTimeline<?>> collections() {
        return Map.of();
    }

    /**
     * Returns the occurrence from which the end is validated.
     *
     * @return start occurrence of the entity
     */
    protected Occurrence lifeStart() {
        return this.start;
    }

    /**
     * Returns the value timelines of the entity.
     *
     * @return timelines by name
     */
    public abstract Map<String, Timeline<?>> timelines();

    /**
     * Returns the name of the entity type.
     *
     * @return type name
     */
    //TODO: Analisar se deveria estar no domínio.
    public abstract String typeName();

    /**
     * Returns a short human-readable label of the entity.
     *
     * @return label
     */
    //TODO: Analisar se deveria estar no domínio.
    public abstract String label();

    /**
     * Returns the name of the agent that owns the entity.
     *
     * @return agent of the start occurrence
     * @throws NullPointerException if the entity has no start
     */
    public String getAgent() {
        return Objects.requireNonNull(this.start, "start").agent();
    }

    /**
     * Tells whether the entity is alive.
     *
     * @return true if no end was recorded
     */
    public boolean isAlive() {
        return this.end == null;
    }

    @Override
    public final int hashCode() {
        return Long.hashCode(this.id);
    }

    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public String toString() {
        return this.typeName() + "#" + this.id + "(" + this.label() + ")";
    }
}
