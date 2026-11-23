package jason.mind.api.core.application.lifecycle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

public class FindEntityLifecycleUseCase implements FindEntityLifecyclePort {

    public static final int DEFAULT_LIMIT = 20;

    private final HistoryQueryRepositoryPort historyQuery;
    private final EffectsDomainService effects;

    public FindEntityLifecycleUseCase(HistoryQueryRepositoryPort historyQuery, EffectsDomainService effects) {
        this.historyQuery = historyQuery;
        this.effects = effects;
    }

    @Override
    public LifecycleOut execute(FindEntityLifecycleIn in) {
        HistoryView.agent(historyQuery, in.agent());
        long id = EntityIds.parse(in.id());
        if (id < 0) throw new EntityNotFoundException(in.agent(), in.id());
        int limit = in.limit() == null ? DEFAULT_LIMIT : in.limit();
        if (limit < 1) throw new InvalidQueryException("limit must be positive");
        Entity<?> e = historyQuery.entityById(in.agent(), id).orElse(null);
        if (e == null) {
            if (historyQuery.wasPruned(in.agent(), id)) throw new EntityPrunedException(in.agent(), in.id());
            throw new EntityNotFoundException(in.agent(), in.id());
        }
        if (!visibleTo(e, in.agent())) throw new EntityNotFoundException(in.agent(), in.id());

        List<Effect> fx = new ArrayList<>();
        for (Effect f : effects.effectsOf(e, historyQuery.causedBy(in.agent(), e)))
            if (visible(f.moment().agent(), f.sequence())) fx.add(f);

        List<LifecycleOut.EntryOut> entries = new ArrayList<>();
        for (OccurrenceRecord r : historyQuery.occurrencesOf(in.agent(), e)) {
            if (!visible(r.occurrence().agent(), r.sequence())) continue;
            if (r.kind() == OccurrenceRecord.Kind.START && e instanceof Message && !r.occurrence().agent().equals(in.agent())) continue;
            String kind = r.kind() == OccurrenceRecord.Kind.ARRIVAL ? "START" : r.kind().name();
            entries.add(new LifecycleOut.EntryOut(r.sequence(), kind, LifecycleCodes.own(e, r, fx),
                    r.kind() == OccurrenceRecord.Kind.CHANGE ? r.timeline() : null,
                    r.kind() == OccurrenceRecord.Kind.CHANGE ? HistoryView.valueText(r.value()) : null,
                    moment(r.occurrence().moment()), cause(r.occurrence().cause()), null, null));
        }
        if (e instanceof Intention intention) {
            List<IntendedMeans> stack = intention.getStack();
            for (int k = 1; k < stack.size(); k++) {
                IntendedMeans im = stack.get(k);
                if (!visible(in.agent(), im.getStart().sequence()) || stack.get(k - 1).getEnd() != null
                        && stack.get(k - 1).getEnd().sequence() <= im.getStart().sequence()) continue;
                entries.add(new LifecycleOut.EntryOut(im.getStart().sequence(), "CHANGE", "INTENTION_SUBGOAL_PUSHED", "stack",
                        EntityIds.format(im), moment(im.getStart().moment()), cause(im.getStart().cause()), null, null));
            }
        }
        java.util.Map<IntendedMeans, Intention> owners = owners(historyQuery, in.agent());
        for (Effect f : fx) {
            if (f.entity() instanceof IntendedMeans im && !isSubgoal(im, owners)) continue;
            String kind = switch (f.kind()) {
                case START -> "GENERATES";
                case CHANGE -> "MODIFIES";
                case END -> "REMOVES";
            };
            boolean subgoal = f.entity() instanceof IntendedMeans im && isSubgoal(im, owners);
            LifecycleOut.EntityOut into = subgoal ? entity(owners.get((IntendedMeans) f.entity())) : null;
            entries.add(new LifecycleOut.EntryOut(f.sequence(), kind, LifecycleCodes.effect(e, f, subgoal), f.timeline(),
                    f.kind() == Effect.Kind.CHANGE ? HistoryView.valueText(f.value()) : null, moment(f.moment()), null,
                    entity(f.entity()), into));
        }
        entries.sort(Comparator.comparingLong(LifecycleOut.EntryOut::sequence));
        long after = in.after() == null ? Long.MIN_VALUE : in.after();
        List<LifecycleOut.EntryOut> page = new ArrayList<>();
        Long next = null;
        for (LifecycleOut.EntryOut en : entries) {
            if (en.sequence() <= after) continue;
            if (page.size() == limit) {
                next = page.get(page.size() - 1).sequence();
                break;
            }
            page.add(en);
        }
        return new LifecycleOut(EntityIds.format(e), e.typeName(), e.label(), page, next);
    }

    private boolean visibleTo(Entity<?> e, String agent) {
        if (e instanceof Message m) {
            if (m.getStart() != null && m.getStart().agent().equals(agent)) return visible(agent, m.getStart().sequence());
            return m.getArrival() != null && visible(agent, m.getArrival().sequence());
        }
        return e.getStart() != null && visible(e.getStart().agent(), e.getStart().sequence());
    }

    private boolean visible(String agent, long sequence) {
        return sequence <= historyQuery.lastClosedCycle(agent).map(CycleRecord::sequenceEnd).orElse(-1L);
    }

    private static java.util.Map<IntendedMeans, Intention> owners(HistoryQueryRepositoryPort query, String agent) {
        java.util.Map<IntendedMeans, Intention> r = new java.util.IdentityHashMap<>();
        for (Entity<?> e : query.entitiesOfTypes(agent, java.util.Set.of("intention")))
            for (IntendedMeans im : ((Intention) e).getStack()) r.put(im, (Intention) e);
        return r;
    }

    private static boolean isSubgoal(IntendedMeans im, java.util.Map<IntendedMeans, Intention> owners) {
        Intention owner = owners.get(im);
        return owner != null && !owner.getStack().isEmpty() && owner.getStack().get(0) != im;
    }

    private static LifecycleOut.MomentOut moment(Moment m) {
        return new LifecycleOut.MomentOut(m.cycle(), m.iteration(), m.getPhase().name(), m.step().name(), m.ordinal());
    }

    private LifecycleOut.CauseOut cause(Cause c) {
        if (c == null) return null;
        Entity<?> by = c.by();
        if (by.getId() > 0 && historyQuery.wasPruned(by.getStart() == null ? ((Message) by).getReceiver() : by.getStart().agent(), by.getId()))
            return null;
        LifecycleOut.LocationOut loc = c.location() == null ? null : new LifecycleOut.LocationOut(EntityIds.format(c.location().plan()),
                c.location().deed(), c.location().line());
        return new LifecycleOut.CauseOut(entity(by), loc);
    }

    private static LifecycleOut.EntityOut entity(Entity<?> e) {
        if (e == null) return null;
        String sender = e instanceof Message m ? m.getSender() : null;
        return new LifecycleOut.EntityOut(e.typeName(), EntityIds.format(e), e.label(), sender);
    }
}
