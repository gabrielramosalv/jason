package jason.mind.api.core.application.lifecycle;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.EntityNotFoundException;
import jason.mind.api.core.application.history.EntityPrunedException;
import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.Effect;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.Cause;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.service.history.EffectsDomainService;
import jason.mind.api.core.ports.in.lifecycle.FindEntityLifecycleIn;
import jason.mind.api.core.ports.in.lifecycle.FindEntityLifecyclePort;
import jason.mind.api.core.ports.in.lifecycle.LifecycleOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Use case that builds the lifecycle of an entity page by page.
 * The lifecycle merges the entity's own occurrences, the subgoals pushed on an intention and the
 * effects the entity caused on other entities, ordered by sequence and limited to closed cycles.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class FindEntityLifecycleUseCase implements FindEntityLifecyclePort {

    /** Page size used when the query gives no limit. */
    public static final int DEFAULT_LIMIT = 20;

    /** Read-only access to the recorded history. */
    private final HistoryQueryRepositoryPort historyQuery;

    /** Domain service that derives the effects caused by an entity. */
    private final EffectsDomainService effects;

    /**
     * Maps every intended means of an agent to the intention whose stack holds it.
     *
     * @param query read-only access to the history
     * @param agent name of the agent
     * @return the owner intention of each intended means, keyed by identity
     */
    private static Map<IntendedMeans, Intention> findOwners(HistoryQueryRepositoryPort query, String agent) {
        Map<IntendedMeans, Intention> r = new IdentityHashMap<>();
        for (Entity e : query.entitiesOfTypes(agent, Set.of("intention"))) {
            for (IntendedMeans im : ((Intention) e).getStack()) {
                r.put(im, (Intention) e);
            }
        }
        return r;
    }

    /**
     * Tells whether an intended means is a subgoal, meaning it is not the bottom of its intention's stack.
     *
     * @param im     intended means being checked
     * @param owners owner intention of each intended means
     * @return {@code true} if the intended means is a subgoal
     */
    private static boolean isSubgoal(IntendedMeans im, Map<IntendedMeans, Intention> owners) {
        Intention owner = owners.get(im);
        return owner != null && !owner.getStack().isEmpty() && owner.getStack().get(0) != im;
    }

    /**
     * Builds the output of a moment.
     *
     * @param m moment to describe
     * @return the moment with cycle, iteration, phase, step and ordinal
     */
    private static LifecycleOut.MomentOut toMoment(Moment m) {
        return new LifecycleOut.MomentOut(m.cycle(), m.iteration(), m.getPhase().name(), m.step().name(), m.ordinal());
    }

    /**
     * Builds the output of an entity, including the sender when it is a message.
     *
     * @param e entity to describe, possibly {@code null}
     * @return the entity output, or {@code null} when the entity is {@code null}
     */
    private static LifecycleOut.EntityOut entity(Entity e) {
        if (e == null) {
            return null;
        }
        String sender = e instanceof Message m ? m.getSender() : null;
        return new LifecycleOut.EntityOut(e.typeName(), EntityIds.format(e), e.label(), sender);
    }

    /**
     * Tells whether an entity can be seen by an agent. A message is visible to its sender once sent
     * and to its receiver once arrived; any other entity is visible once its start is in a closed cycle.
     *
     * @param e     entity being checked
     * @param agent name of the agent being queried
     * @return {@code true} if the entity is visible
     */
    private boolean visibleTo(Entity e, String agent) {
        if (e instanceof Message m) {
            if (m.getStart() != null && m.getStart().agent().equals(agent)) {
                return this.isVisible(agent, m.getStart().sequence());
            }
            return m.getArrival() != null && this.isVisible(agent, m.getArrival().sequence());
        }
        return e.getStart() != null && this.isVisible(e.getStart().agent(), e.getStart().sequence());
    }

    /**
     * Tells whether a sequence of an agent belongs to a closed cycle.
     *
     * @param agent    name of the agent that owns the sequence
     * @param sequence sequence being checked
     * @return {@code true} if the sequence is not after the last closed cycle of the agent
     */
    private boolean isVisible(String agent, long sequence) {
        return sequence <= this.historyQuery.lastClosedCycle(agent).map(CycleRecord::sequenceEnd).orElse(-1L);
    }

    /**
     * Builds the output of a cause, hiding causes whose entity was pruned.
     *
     * @param c cause to describe, possibly {@code null}
     * @return the cause with its entity and code location, or {@code null} when there is no cause or its entity was
     * pruned
     */
    private LifecycleOut.CauseOut describeCause(Cause c) {
        if (c == null) {
            return null;
        }
        Entity by = c.by();
        if (by.getId() > 0 && this.historyQuery.wasPruned(
                by.getStart() == null ? ((Message) by).getReceiver() : by.getStart().agent(), by.getId())) {
            return null;
        }
        LifecycleOut.LocationOut loc = c.location() == null ? null : new LifecycleOut.LocationOut(
                EntityIds.format(c.location().plan()), c.location().deed(), c.location().line());
        return new LifecycleOut.CauseOut(entity(by), loc);
    }

    @Override
    public LifecycleOut execute(FindEntityLifecycleIn in) {
        HistoryView.agent(this.historyQuery, in.agent());
        long id = EntityIds.parse(in.id());
        if (id < 0) {
            throw new EntityNotFoundException(in.agent(), in.id());
        }
        int limit = in.limit() == null ? DEFAULT_LIMIT : in.limit();
        if (limit < 1) {
            throw new InvalidQueryException("limit must be positive");
        }
        Entity e = this.historyQuery.entityById(in.agent(), id).orElse(null);
        if (e == null) {
            if (this.historyQuery.wasPruned(in.agent(), id)) {
                throw new EntityPrunedException(in.agent(), in.id());
            }
            throw new EntityNotFoundException(in.agent(), in.id());
        }
        if (!this.visibleTo(e, in.agent())) {
            throw new EntityNotFoundException(in.agent(), in.id());
        }

        List<Effect> fx = new ArrayList<>();
        for (Effect f : this.effects.effectsOf(e, this.historyQuery.causedBy(in.agent(), e))) {
            if (this.isVisible(f.moment().agent(), f.sequence())) {
                fx.add(f);
            }
        }

        List<LifecycleOut.EntryOut> entries = new ArrayList<>();
        for (OccurrenceRecord r : this.historyQuery.occurrencesOf(in.agent(), e)) {
            if (!this.isVisible(r.occurrence().agent(), r.sequence())) {
                continue;
            }
            if (r.kind() == OccurrenceRecord.Kind.START && e instanceof Message && !r.occurrence().agent().equals(
                    in.agent())) {
                continue;
            }
            String kind = r.kind() == OccurrenceRecord.Kind.ARRIVAL ? "START" : r.kind().name();
            entries.add(new LifecycleOut.EntryOut(r.sequence(), kind, LifecycleCodes.describeOwn(e, r, fx),
                    r.kind() == OccurrenceRecord.Kind.CHANGE ? r.timeline() : null,
                    r.kind() == OccurrenceRecord.Kind.CHANGE ? HistoryView.formatValue(r.value()) : null,
                    toMoment(r.occurrence().moment()), this.describeCause(r.occurrence().cause()), null, null));
        }
        if (e instanceof Intention intention) {
            List<IntendedMeans> stack = intention.getStack();
            for (int k = 1; k < stack.size(); k++) {
                IntendedMeans im = stack.get(k);
                if (!this.isVisible(in.agent(), im.getStart().sequence()) || stack.get(k - 1).getEnd() != null
                        && stack.get(k - 1).getEnd().sequence() <= im.getStart().sequence()) {
                    continue;
                }
                entries.add(new LifecycleOut.EntryOut(im.getStart().sequence(), "CHANGE", "INTENTION_SUBGOAL_PUSHED",
                        "stack", EntityIds.format(im), toMoment(im.getStart().moment()),
                        this.describeCause(im.getStart().cause()), null, null));
            }
        }
        Map<IntendedMeans, Intention> owners = findOwners(this.historyQuery, in.agent());
        for (Effect f : fx) {
            if (f.entity() instanceof IntendedMeans im && !isSubgoal(im, owners)) {
                continue;
            }
            String kind = switch (f.kind()) {
                case START -> "GENERATES";
                case CHANGE -> "MODIFIES";
                case END -> "REMOVES";
            };
            boolean subgoal = f.entity() instanceof IntendedMeans im && isSubgoal(im, owners);
            LifecycleOut.EntityOut into = subgoal ? entity(owners.get((IntendedMeans) f.entity())) : null;
            entries.add(
                    new LifecycleOut.EntryOut(f.sequence(), kind, LifecycleCodes.describeEffect(e, f, subgoal), f.timeline(),
                            f.kind() == Effect.Kind.CHANGE ? HistoryView.formatValue(f.value()) : null,
                            toMoment(f.moment()), null, entity(f.entity()), into));
        }
        entries.sort(Comparator.comparingLong(LifecycleOut.EntryOut::sequence));
        long after = in.after() == null ? Long.MIN_VALUE : in.after();
        List<LifecycleOut.EntryOut> page = new ArrayList<>();
        Long next = null;
        for (LifecycleOut.EntryOut en : entries) {
            if (en.sequence() <= after) {
                continue;
            }
            if (page.size() == limit) {
                next = page.get(page.size() - 1).sequence();
                break;
            }
            page.add(en);
        }
        return new LifecycleOut(EntityIds.format(e), e.typeName(), e.label(), page, next);
    }
}
