package jason.mind.api.infrastructure.adapters.out.persistence.memory.history;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import jason.mind.api.core.domain.model.entities.Binding;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.AppendOnlyList;
import jason.mind.api.core.domain.model.timeline.CollectionTimeline;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.domain.model.timeline.Timeline;
import jason.mind.api.core.domain.model.trace.PlanSelection;

final class AgentStore {

    volatile Agent agent;
    final Map<Long, Entity<?>> entities = new ConcurrentHashMap<>();
    final Map<Long, Message> incoming = new ConcurrentHashMap<>();
    final ConcurrentSkipListMap<Integer, CycleRecord> cycles = new ConcurrentSkipListMap<>();
    final ConcurrentSkipListMap<Integer, AppendOnlyList<OccurrenceRecord>> occurrencesByCycle = new ConcurrentSkipListMap<>();
    final Map<Long, AppendOnlyList<OccurrenceRecord>> occurrencesByEntity = new ConcurrentHashMap<>();
    final Map<Long, AppendOnlyList<OccurrenceRecord>> occurrencesByCause = new ConcurrentHashMap<>();
    final ConcurrentSkipListMap<Integer, AppendOnlyList<PlanSelection>> selectionsByCycle = new ConcurrentSkipListMap<>();
    final Map<Long, AppendOnlyList<PlanSelection>> selectionsByEvent = new ConcurrentHashMap<>();
    final Map<String, AppendOnlyList<Moment>> deedExecutions = new ConcurrentHashMap<>();
    private final BitSet pruned = new BitSet();
    volatile CycleRecord lastClosed;
    volatile int horizon = Integer.MIN_VALUE;
    private int lastScanHorizon = Integer.MIN_VALUE;

    AgentStore(Agent agent) {
        this.agent = agent;
    }

    void append(OccurrenceRecord r) {
        Occurrence o = r.occurrence();
        occurrencesByCycle.computeIfAbsent(o.cycle(), k -> new AppendOnlyList<>()).add(r);
        occurrencesByEntity.computeIfAbsent(r.entity().getId(), k -> new AppendOnlyList<>()).add(r);
        if (o.cause() != null)
            occurrencesByCause.computeIfAbsent(o.cause().by().getId(), k -> new AppendOnlyList<>()).add(r);
        if (r.entity() instanceof IntendedMeans im && "currentDeed".equals(r.timeline()) && r.value() instanceof Integer deed
                && o.moment().step() == ReasoningStep.EXECUTE_INTENTION && im.getPlan() != null)
            deedExecutions.computeIfAbsent(deedKey(im.getPlan().getId(), deed), k -> new AppendOnlyList<>()).add(o.moment());
    }

    void append(PlanSelection s) {
        selectionsByCycle.computeIfAbsent(s.moment().cycle(), k -> new AppendOnlyList<>()).add(s);
        selectionsByEvent.computeIfAbsent(s.event().getId(), k -> new AppendOnlyList<>()).add(s);
    }

    static String deedKey(long planId, int deedIndex) {
        return planId + ":" + deedIndex;
    }

    boolean wasPruned(long id) {
        synchronized (pruned) {
            if (id <= Integer.MAX_VALUE && pruned.get((int) id)) return true;
        }
        Entity<?> e = entities.get(id);
        if (e == null) e = incoming.get(id);
        return e != null && deadBeforeHorizon(e);
    }

    boolean deadBeforeHorizon(Entity<?> e) {
        Occurrence end = lastOwnOccurrence(e);
        return end != null && end.cycle() < horizon;
    }

    private Occurrence lastOwnOccurrence(Entity<?> e) {
        if (e instanceof Message m) {
            Occurrence end = m.getEnd();
            if (end == null) return null;
            String name = agent.getName();
            if (m.getStart() != null && m.getStart().agent().equals(name) && !m.getReceiver().equals(name)) return m.getStart();
            return end.agent().equals(name) ? end : null;
        }
        return e.getEnd();
    }

    void prune(int newHorizon) {
        if (newHorizon <= horizon) return;
        horizon = newHorizon;
        cycles.headMap(newHorizon).clear();
        occurrencesByCycle.headMap(newHorizon).clear();
        selectionsByCycle.headMap(newHorizon).clear();
        int retained = lastClosed == null ? 1 : Math.max(1, lastClosed.cycle() - newHorizon + 1);
        if (lastScanHorizon != Integer.MIN_VALUE && newHorizon - lastScanHorizon < Math.max(1, retained / 10)) return;
        lastScanHorizon = newHorizon;
        scan(newHorizon);
    }

    private void scan(int h) {
        List<Long> dead = new ArrayList<>();
        for (Entity<?> e : entities.values()) {
            if (deadBeforeHorizon(e)) dead.add(e.getId());
            else pruneTimelines(e, h);
        }
        for (Message m : incoming.values())
            if (deadBeforeHorizon(m)) dead.add(m.getId());
        synchronized (pruned) {
            for (long id : dead) if (id <= Integer.MAX_VALUE) pruned.set((int) id);
        }
        for (long id : dead) {
            entities.remove(id);
            incoming.remove(id);
            occurrencesByEntity.remove(id);
            occurrencesByCause.remove(id);
            selectionsByEvent.remove(id);
        }
        for (Map.Entry<Long, AppendOnlyList<OccurrenceRecord>> en : occurrencesByEntity.entrySet())
            en.getValue().replaceAll(keep(en.getValue().snapshot(), h, true));
        for (Map.Entry<Long, AppendOnlyList<OccurrenceRecord>> en : occurrencesByCause.entrySet())
            en.getValue().replaceAll(keep(en.getValue().snapshot(), h, false));
        for (AppendOnlyList<PlanSelection> l : selectionsByEvent.values()) {
            List<PlanSelection> kept = new ArrayList<>();
            for (PlanSelection s : l.snapshot()) if (s.moment().cycle() >= h) kept.add(s);
            l.replaceAll(kept);
        }
        for (AppendOnlyList<Moment> l : deedExecutions.values()) {
            List<Moment> kept = new ArrayList<>();
            for (Moment m : l.snapshot()) if (m.cycle() >= h) kept.add(m);
            l.replaceAll(kept);
        }
    }

    private static List<OccurrenceRecord> keep(List<OccurrenceRecord> list, int h, boolean keepStart) {
        List<OccurrenceRecord> kept = new ArrayList<>();
        for (OccurrenceRecord r : list)
            if (r.occurrence().cycle() >= h || keepStart && r.kind() == OccurrenceRecord.Kind.START) kept.add(r);
        return kept;
    }

    private static void pruneTimelines(Entity<?> e, int h) {
        for (Timeline<?> t : e.timelines().values()) t.pruneBefore(h);
        for (CollectionTimeline<?> c : e.collections().values()) c.pruneBefore(h);
        if (e instanceof IntendedMeans im) pruneBindings(im, h);
        if (e instanceof Intention i) i.pruneStack(h);
    }

    private static void pruneBindings(IntendedMeans im, int h) {
        List<Binding> all = im.getBindings();
        if (all.isEmpty() || all.get(0).moment().cycle() >= h) return;
        Map<String, Binding> lastBefore = new java.util.LinkedHashMap<>();
        List<Binding> after = new ArrayList<>();
        for (Binding b : all) {
            if (b.moment().cycle() < h) {
                lastBefore.remove(b.name());
                lastBefore.put(b.name(), b);
            } else after.add(b);
        }
        List<Binding> kept = new ArrayList<>(lastBefore.values());
        kept.addAll(after);
        im.replaceBindings(kept);
    }
}
