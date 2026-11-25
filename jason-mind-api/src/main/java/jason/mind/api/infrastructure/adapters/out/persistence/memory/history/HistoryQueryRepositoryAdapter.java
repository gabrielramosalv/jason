package jason.mind.api.infrastructure.adapters.out.persistence.memory.history;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.timeline.Cause;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.AgentCycle;
import jason.mind.api.core.ports.out.history.Footprint;
import jason.mind.api.core.domain.model.timeline.CollectionTimeline;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.domain.model.timeline.Timeline;
import jason.mind.api.core.domain.model.timeline.TimelineEntry;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter that answers history queries from the in-memory history.
 * Only exposes data up to the last closed cycle of each agent and within its retention horizon. Occurrences, plan
 * selections and deed executions are rebuilt from the entities, which keep their own history.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class HistoryQueryRepositoryAdapter implements HistoryQueryRepositoryPort {

    /** In-memory history of all agents. */
    private final HistoryMemory memory;

    /**
     * Returns the last sequence visible to queries, which is the end of the last closed cycle.
     *
     * @param s store of the agent
     * @return last visible sequence, or {@code -1} when no cycle was closed yet
     */
    private static long cut(AgentStore s) {
        CycleRecord c = s.lastClosed;
        return c == null ? -1 : c.sequenceEnd();
    }

    /**
     * Tells whether a store holds an entity, as an own entity or as an incoming message.
     *
     * @param s      store of the agent
     * @param entity entity looked up
     * @return {@code true} when the store holds this very entity
     */
    private static boolean holds(AgentStore s, Entity entity) {
        long id = entity.getId();
        return s.entities.get(id) == entity || s.incoming.get(id) == entity;
    }

    /**
     * Tells whether an occurrence happened within a range of cycles.
     *
     * @param o    occurrence to check
     * @param from first cycle of the range
     * @param to   last cycle of the range
     * @return {@code true} when the cycle of the occurrence is within the range
     */
    private static boolean within(Occurrence o, int from, int to) {
        return o.cycle() >= from && o.cycle() <= to;
    }

    /**
     * Rebuilds the occurrences of an entity recorded in an agent within a range of cycles: its start, the changes of
     * its timelines and collections, its arrival and its end. The values set by the start itself are not changes.
     *
     * @param e         entity whose occurrences are rebuilt
     * @param agent     name of the agent where the occurrences happened
     * @param from      first cycle of the range
     * @param to        last cycle of the range
     * @param keepStart whether the start is included even when it is outside the range
     * @param cut       last visible sequence
     * @param out       list that receives the occurrences, in no particular order
     */
    private static void collect(Entity e, String agent, int from, int to, boolean keepStart, long cut,
                                List<OccurrenceRecord> out) {
        Occurrence start = e.getStart();
        if (start != null && start.agent().equals(agent) && start.sequence() <= cut
                && (keepStart || within(start, from, to))) {
            out.add(new OccurrenceRecord(e, OccurrenceRecord.Kind.START, null, null, start));
        }
        for (Map.Entry<String, Timeline<?>> t : e.timelines().entrySet()) {
            for (TimelineEntry<?> en : t.getValue().entriesSince(from)) {
                Occurrence o = en.occurrence();
                if (o.cycle() > to) {
                    break;
                }
                if (o != start && o.sequence() <= cut) {
                    out.add(new OccurrenceRecord(e, OccurrenceRecord.Kind.CHANGE, t.getKey(), en.value(), o));
                }
            }
        }
        for (Map.Entry<String, CollectionTimeline<?>> c : e.collections().entrySet()) {
            for (TimelineEntry<?> en : c.getValue().operationsSince(from)) {
                Occurrence o = en.occurrence();
                if (o.cycle() > to) {
                    break;
                }
                if (o != start && o.sequence() <= cut) {
                    out.add(new OccurrenceRecord(e, OccurrenceRecord.Kind.CHANGE, c.getKey(), en.value(), o));
                }
            }
        }
        if (e instanceof Message m) {
            Occurrence a = m.getArrival();
            if (a != null && a.agent().equals(agent) && a.sequence() <= cut && within(a, from, to)) {
                out.add(new OccurrenceRecord(e, OccurrenceRecord.Kind.ARRIVAL, null, null, a));
            }
        }
        Occurrence end = e.getEnd();
        if (end != null && end.agent().equals(agent) && end.sequence() <= cut && within(end, from, to)) {
            out.add(new OccurrenceRecord(e, OccurrenceRecord.Kind.END, null, null, end));
        }
    }

    /**
     * Finds the stores that hold occurrences of an entity: the sender and receiver stores for a message,
     * or the store of the given agent otherwise.
     *
     * @param agent  name of the agent that owns the entity
     * @param entity entity whose stores are wanted
     * @return distinct stores holding occurrences of the entity
     */
    private List<AgentStore> storesOf(String agent, Entity entity) {
        List<AgentStore> r = new ArrayList<>();
        if (entity instanceof Message m) {
            Map<AgentStore, Boolean> seen = new IdentityHashMap<>();
            for (String name : new String[]{m.getSender(), m.getReceiver()}) {
                AgentStore s = name == null ? null : this.memory.findStore(name);
                if (s != null && seen.put(s, true) == null) {
                    r.add(s);
                }
            }
        } else {
            AgentStore s = this.memory.findStore(agent);
            if (s != null) {
                r.add(s);
            }
        }
        return r;
    }

    @Override
    public List<Agent> agents() {
        List<Agent> r = new ArrayList<>();
        for (AgentStore s : this.memory.stores.values()) {
            r.add(s.agent);
        }
        return r;
    }

    @Override
    public Optional<Agent> agent(String name) {
        AgentStore s = this.memory.findStore(name);
        return s == null ? Optional.empty() : Optional.of(s.agent);
    }

    @Override
    public Footprint footprint(String agent) {
        AgentStore s = this.memory.findStore(agent);
        return s == null ? new Footprint(0, 0, 0) : s.footprint();
    }

    @Override
    public Optional<CycleRecord> lastClosedCycle(String agent) {
        AgentStore s = this.memory.findStore(agent);
        return s == null ? Optional.empty() : Optional.ofNullable(s.lastClosed);
    }

    @Override
    public Optional<CycleRecord> oldestCycle(String agent) {
        AgentStore s = this.memory.findStore(agent);
        if (s == null || s.lastClosed == null) {
            return Optional.empty();
        }
        Map.Entry<Integer, CycleRecord> e = s.cycles.ceilingEntry(Math.max(s.horizon, Integer.MIN_VALUE));
        return e == null ? Optional.empty() : Optional.of(e.getValue());
    }

    @Override
    public Optional<CycleRecord> cycle(String agent, int cycle) {
        AgentStore s = this.memory.findStore(agent);
        if (s == null || cycle < s.horizon) {
            return Optional.empty();
        }
        return Optional.ofNullable(s.cycles.get(cycle));
    }

    @Override
    public List<CycleRecord> cycles(String agent, int from, int to) {
        AgentStore s = this.memory.findStore(agent);
        if (s == null || from > to) {
            return List.of();
        }
        return new ArrayList<>(s.cycles.subMap(Math.max(from, s.horizon), true, to, true).values());
    }

    @Override
    public List<Entity> entitiesAt(String agent, int cycle) {
        AgentStore s = this.memory.findStore(agent);
        if (s == null) {
            return List.of();
        }
        long cut = cut(s);
        AgentCycle ac = new AgentCycle(agent, cycle);
        List<Entity> r = new ArrayList<>();
        for (Entity e : s.entities.values()) {
            if (e.getStart() == null || e.getStart().sequence() > cut || s.deadBeforeHorizon(e)) {
                continue;
            }
            if (e.getStart().cycle() > cycle) {
                continue;
            }
            if (e.getEnd() != null && e.getEnd().cycle() < cycle) {
                continue;
            }
            r.add(e);
        }
        for (Message m : s.incoming.values()) {
            if (m.existsAt(ac) && !s.deadBeforeHorizon(m)) {
                r.add(m);
            }
        }
        r.sort(Comparator.comparingLong(Entity::getId));
        return r;
    }

    @Override
    public List<OccurrenceRecord> occurrencesAt(String agent, int cycle) {
        AgentStore s = this.memory.findStore(agent);
        if (s == null || cycle < s.horizon) {
            return List.of();
        }
        long cut = cut(s);
        String name = s.agent.getName();
        List<OccurrenceRecord> r = new ArrayList<>();
        for (Entity e : s.entities.values()) {
            collect(e, name, cycle, cycle, false, cut, r);
        }
        for (Message m : s.incoming.values()) {
            if (s.entities.get(m.getId()) != m) {
                collect(m, name, cycle, cycle, false, cut, r);
            }
        }
        r.sort(Comparator.comparingLong(OccurrenceRecord::sequence));
        return r;
    }

    @Override
    public List<OccurrenceRecord> causedBy(String agent, Entity entity) {
        List<OccurrenceRecord> r = new ArrayList<>();
        for (AgentStore s : storesOf(agent, entity)) {
            if (!holds(s, entity)) {
                continue;
            }
            long cut = cut(s);
            int from = s.lastScanHorizon;
            String name = s.agent.getName();
            // derived from the entities: every occurrence of the store whose cause is the entity (D-084)
            List<OccurrenceRecord> all = new ArrayList<>();
            for (Entity e : s.entities.values()) {
                collect(e, name, from, Integer.MAX_VALUE, false, cut, all);
            }
            for (Message m : s.incoming.values()) {
                if (s.entities.get(m.getId()) != m) {
                    collect(m, name, from, Integer.MAX_VALUE, false, cut, all);
                }
            }
            for (OccurrenceRecord rec : all) {
                Cause c = rec.occurrence().cause();
                if (c != null && c.by() == entity) {
                    r.add(rec);
                }
            }
        }
        r.sort(Comparator.comparingLong(OccurrenceRecord::sequence));
        return r;
    }

    @Override
    public List<OccurrenceRecord> occurrencesOf(String agent, Entity entity) {
        List<OccurrenceRecord> r = new ArrayList<>();
        for (AgentStore s : storesOf(agent, entity)) {
            if (!holds(s, entity)) {
                continue;
            }
            List<OccurrenceRecord> own = new ArrayList<>();
            collect(entity, s.agent.getName(), s.lastScanHorizon, Integer.MAX_VALUE, true, cut(s), own);
            own.sort(Comparator.comparingLong(OccurrenceRecord::sequence));
            r.addAll(own);
        }
        r.sort(Comparator.comparingLong(OccurrenceRecord::sequence));
        return r;
    }

    @Override
    public List<PlanSelection> planSelectionsAt(String agent, int cycle) {
        AgentStore s = this.memory.findStore(agent);
        if (s == null || cycle < s.horizon) {
            return List.of();
        }
        long cut = cut(s);
        List<PlanSelection> r = new ArrayList<>();
        for (Entity e : s.entities.values()) {
            if (e instanceof Event ev) {
                for (PlanSelection ps : ev.getSelections()) {
                    if (ps.moment().cycle() == cycle && ps.sequence() <= cut) {
                        r.add(ps);
                    }
                }
            }
        }
        r.sort(Comparator.comparingLong(PlanSelection::sequence));
        return r;
    }

    @Override
    public List<Moment> deedExecutions(String agent, Plan plan, int deedIndex) {
        AgentStore s = this.memory.findStore(agent);
        CycleRecord last = s == null ? null : s.lastClosed;
        if (last == null) {
            return List.of();
        }
        List<Occurrence> found = new ArrayList<>();
        for (Entity e : s.entities.values()) {
            if (!(e instanceof IntendedMeans im) || im.getPlan() == null || im.getPlan().getId() != plan.getId()) {
                continue;
            }
            for (TimelineEntry<Integer> en : im.getCurrentDeed().entriesSince(s.horizon)) {
                Occurrence o = en.occurrence();
                if (o.cycle() > last.cycle()) {
                    break;
                }
                if (en.value() != null && en.value() == deedIndex
                        && o.moment().step() == ReasoningStep.EXECUTE_INTENTION) {
                    found.add(o);
                }
            }
        }
        found.sort(Comparator.comparingLong(Occurrence::sequence));
        List<Moment> r = new ArrayList<>(found.size());
        for (Occurrence o : found) {
            r.add(o.moment());
        }
        return r;
    }

    @Override
    public List<Entity> entitiesOfTypes(String agent, Set<String> types) {
        AgentStore s = this.memory.findStore(agent);
        if (s == null) {
            return List.of();
        }
        List<Entity> r = new ArrayList<>();
        for (Entity e : s.entities.values()) {
            if (types.contains(e.typeName()) && !s.deadBeforeHorizon(e)) {
                r.add(e);
            }
        }
        if (types.contains("message")) {
            for (Message m : s.incoming.values()) {
                if (!s.deadBeforeHorizon(m)) {
                    r.add(m);
                }
            }
        }
        r.sort(Comparator.comparingLong(Entity::getId));
        return r;
    }

    @Override
    public Optional<Entity> entityById(String agent, long id) {
        AgentStore s = this.memory.findStore(agent);
        if (s == null) {
            return Optional.empty();
        }
        Entity e = s.entities.get(id);
        if (e == null) {
            e = s.incoming.get(id);
        }
        if (e == null || s.deadBeforeHorizon(e)) {
            return Optional.empty();
        }
        return Optional.of(e);
    }

    @Override
    public boolean wasPruned(String agent, long id) {
        AgentStore s = this.memory.findStore(agent);
        return s != null && s.wasPruned(id);
    }

    @Override
    public Collection<Message> messages() {
        Map<Message, Boolean> all = new IdentityHashMap<>();
        for (AgentStore s : this.memory.stores.values()) {
            for (Entity e : s.entities.values()) {
                if (e instanceof Message m && !s.deadBeforeHorizon(m)) {
                    all.put(m, true);
                }
            }
            for (Message m : s.incoming.values()) {
                if (!s.deadBeforeHorizon(m)) {
                    all.put(m, true);
                }
            }
        }
        List<Message> r = new ArrayList<>(all.keySet());
        r.sort(Comparator.comparingLong(Entity::getId));
        return r;
    }
}
