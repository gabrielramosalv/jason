package jason.mind.api.infrastructure.adapters.out.persistence.memory.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.AgentCycle;
import jason.mind.api.core.domain.model.timeline.AppendOnlyList;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

public class HistoryQueryRepositoryAdapter implements HistoryQueryRepositoryPort {

    private final HistoryMemory memory;

    public HistoryQueryRepositoryAdapter(HistoryMemory memory) {
        this.memory = memory;
    }

    @Override
    public List<Agent> agents() {
        List<Agent> r = new ArrayList<>();
        for (AgentStore s : memory.stores.values()) r.add(s.agent);
        return r;
    }

    @Override
    public Optional<Agent> agent(String name) {
        AgentStore s = memory.store(name);
        return s == null ? Optional.empty() : Optional.of(s.agent);
    }

    @Override
    public Optional<CycleRecord> lastClosedCycle(String agent) {
        AgentStore s = memory.store(agent);
        return s == null ? Optional.empty() : Optional.ofNullable(s.lastClosed);
    }

    @Override
    public Optional<CycleRecord> oldestCycle(String agent) {
        AgentStore s = memory.store(agent);
        if (s == null || s.lastClosed == null) return Optional.empty();
        Map.Entry<Integer, CycleRecord> e = s.cycles.ceilingEntry(Math.max(s.horizon, Integer.MIN_VALUE));
        return e == null ? Optional.empty() : Optional.of(e.getValue());
    }

    @Override
    public Optional<CycleRecord> cycle(String agent, int cycle) {
        AgentStore s = memory.store(agent);
        if (s == null || cycle < s.horizon) return Optional.empty();
        return Optional.ofNullable(s.cycles.get(cycle));
    }

    @Override
    public List<CycleRecord> cycles(String agent, int from, int to) {
        AgentStore s = memory.store(agent);
        if (s == null || from > to) return List.of();
        return new ArrayList<>(s.cycles.subMap(Math.max(from, s.horizon), true, to, true).values());
    }

    private static long cut(AgentStore s) {
        CycleRecord c = s.lastClosed;
        return c == null ? -1 : c.sequenceEnd();
    }

    @Override
    public List<Entity<?>> entitiesAt(String agent, int cycle) {
        AgentStore s = memory.store(agent);
        if (s == null) return List.of();
        long cut = cut(s);
        AgentCycle ac = new AgentCycle(agent, cycle);
        List<Entity<?>> r = new ArrayList<>();
        for (Entity<?> e : s.entities.values()) {
            if (e.getStart() == null || e.getStart().sequence() > cut || s.deadBeforeHorizon(e)) continue;
            if (e.getStart().cycle() > cycle) continue;
            if (e.getEnd() != null && e.getEnd().cycle() < cycle) continue;
            r.add(e);
        }
        for (Message m : s.incoming.values()) if (m.existsAt(ac) && !s.deadBeforeHorizon(m)) r.add(m);
        r.sort(Comparator.comparingLong(Entity::getId));
        return r;
    }

    @Override
    public List<OccurrenceRecord> occurrencesAt(String agent, int cycle) {
        AgentStore s = memory.store(agent);
        if (s == null) return List.of();
        AppendOnlyList<OccurrenceRecord> l = s.occurrencesByCycle.get(cycle);
        if (l == null) return List.of();
        long cut = cut(s);
        List<OccurrenceRecord> r = new ArrayList<>();
        for (OccurrenceRecord rec : l.snapshot()) if (rec.sequence() <= cut) r.add(rec);
        return r;
    }

    @Override
    public List<OccurrenceRecord> causedBy(String agent, Entity<?> entity) {
        List<OccurrenceRecord> r = new ArrayList<>();
        for (AgentStore s : storesOf(agent, entity)) {
            AppendOnlyList<OccurrenceRecord> l = s.occurrencesByCause.get(entity.getId());
            if (l == null) continue;
            long cut = cut(s);
            for (OccurrenceRecord rec : l.snapshot()) if (rec.sequence() <= cut) r.add(rec);
        }
        r.sort(Comparator.comparingLong(OccurrenceRecord::sequence));
        return r;
    }

    @Override
    public List<OccurrenceRecord> occurrencesOf(String agent, Entity<?> entity) {
        List<OccurrenceRecord> r = new ArrayList<>();
        for (AgentStore s : storesOf(agent, entity)) {
            AppendOnlyList<OccurrenceRecord> l = s.occurrencesByEntity.get(entity.getId());
            if (l == null) continue;
            long cut = cut(s);
            for (OccurrenceRecord rec : l.snapshot()) if (rec.sequence() <= cut) r.add(rec);
        }
        r.sort(Comparator.comparingLong(OccurrenceRecord::sequence));
        return r;
    }

    private List<AgentStore> storesOf(String agent, Entity<?> entity) {
        List<AgentStore> r = new ArrayList<>();
        if (entity instanceof Message m) {
            Map<AgentStore, Boolean> seen = new IdentityHashMap<>();
            for (String name : new String[] { m.getSender(), m.getReceiver() }) {
                AgentStore s = name == null ? null : memory.store(name);
                if (s != null && seen.put(s, true) == null) r.add(s);
            }
        } else {
            AgentStore s = memory.store(agent);
            if (s != null) r.add(s);
        }
        return r;
    }

    @Override
    public List<PlanSelection> planSelectionsAt(String agent, int cycle) {
        AgentStore s = memory.store(agent);
        if (s == null) return List.of();
        AppendOnlyList<PlanSelection> l = s.selectionsByCycle.get(cycle);
        if (l == null) return List.of();
        long cut = cut(s);
        List<PlanSelection> r = new ArrayList<>();
        for (PlanSelection ps : l.snapshot()) if (ps.sequence() <= cut) r.add(ps);
        return r;
    }

    @Override
    public List<PlanSelection> planSelectionsOf(String agent, Event event) {
        AgentStore s = memory.store(agent);
        if (s == null) return List.of();
        AppendOnlyList<PlanSelection> l = s.selectionsByEvent.get(event.getId());
        if (l == null) return List.of();
        long cut = cut(s);
        List<PlanSelection> r = new ArrayList<>();
        for (PlanSelection ps : l.snapshot()) if (ps.sequence() <= cut) r.add(ps);
        return r;
    }

    @Override
    public List<Moment> deedExecutions(String agent, Plan plan, int deedIndex) {
        AgentStore s = memory.store(agent);
        if (s == null) return List.of();
        AppendOnlyList<Moment> l = s.deedExecutions.get(AgentStore.deedKey(plan.getId(), deedIndex));
        if (l == null) return List.of();
        CycleRecord last = s.lastClosed;
        List<Moment> r = new ArrayList<>();
        for (Moment m : l.snapshot()) if (last != null && m.cycle() <= last.cycle() && m.cycle() >= s.horizon) r.add(m);
        return r;
    }

    @Override
    public List<Entity<?>> entitiesOfTypes(String agent, Set<String> types) {
        AgentStore s = memory.store(agent);
        if (s == null) return List.of();
        List<Entity<?>> r = new ArrayList<>();
        for (Entity<?> e : s.entities.values())
            if (types.contains(e.typeName()) && !s.deadBeforeHorizon(e)) r.add(e);
        if (types.contains("message"))
            for (Message m : s.incoming.values()) if (!s.deadBeforeHorizon(m)) r.add(m);
        r.sort(Comparator.comparingLong(Entity::getId));
        return r;
    }

    @Override
    public Optional<Entity<?>> entityById(String agent, long id) {
        AgentStore s = memory.store(agent);
        if (s == null) return Optional.empty();
        Entity<?> e = s.entities.get(id);
        if (e == null) e = s.incoming.get(id);
        if (e == null || s.deadBeforeHorizon(e)) return Optional.empty();
        return Optional.of(e);
    }

    @Override
    public boolean wasPruned(String agent, long id) {
        AgentStore s = memory.store(agent);
        return s != null && s.wasPruned(id);
    }

    @Override
    public Collection<Message> messages() {
        Map<Message, Boolean> all = new IdentityHashMap<>();
        for (AgentStore s : memory.stores.values()) {
            for (Entity<?> e : s.entities.values()) if (e instanceof Message m && !s.deadBeforeHorizon(m)) all.put(m, true);
            for (Message m : s.incoming.values()) if (!s.deadBeforeHorizon(m)) all.put(m, true);
        }
        List<Message> r = new ArrayList<>(all.keySet());
        r.sort(Comparator.comparingLong(Entity::getId));
        return r;
    }
}
