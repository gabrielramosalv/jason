package jason.mind.api.core.application.agent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.domain.model.entities.Belief;
import jason.mind.api.core.domain.model.entities.Binding;
import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.domain.model.timeline.TimelineEntry;
import jason.mind.api.core.domain.service.reasoning.QueueDomainService;
import jason.mind.api.core.ports.in.agent.AgentStateOut;
import jason.mind.api.core.ports.in.agent.FindAgentStateIn;
import jason.mind.api.core.ports.in.agent.FindAgentStatePort;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

public class FindAgentStateUseCase implements FindAgentStatePort {

    public static final Set<String> PARTS = Set.of("beliefs", "intentions", "events");

    private final HistoryQueryRepositoryPort historyQuery;
    private final QueueDomainService queue;

    public FindAgentStateUseCase(HistoryQueryRepositoryPort historyQuery, QueueDomainService queue) {
        this.historyQuery = historyQuery;
        this.queue = queue;
    }

    @Override
    public AgentStateOut execute(FindAgentStateIn in) {
        HistoryView.agent(historyQuery, in.agent());
        Set<String> include = in.include() == null || in.include().isEmpty() ? PARTS : in.include();
        for (String p : include)
            if (!PARTS.contains(p)) throw new InvalidQueryException("unknown include: " + p);
        CycleRecord oldest = historyQuery.oldestCycle(in.agent()).orElse(null);
        CycleRecord newest = historyQuery.lastClosedCycle(in.agent()).orElse(null);
        if (oldest == null || newest == null)
            return new AgentStateOut(in.agent(), null, false, null, null, null,
                    include.contains("beliefs") ? List.of() : null, include.contains("intentions") ? List.of() : null,
                    include.contains("events") ? List.of() : null, include.contains("intentions") ? List.of() : null);
        int requested = in.cycle() == null ? newest.cycle() : in.cycle();
        int cycle = Math.max(oldest.cycle(), Math.min(newest.cycle(), requested));
        boolean clamped = cycle != requested;
        CycleRecord record = historyQuery.cycle(in.agent(), cycle).orElse(newest);
        long cut = record.sequenceEnd();
        List<Entity<?>> entities = historyQuery.entitiesAt(in.agent(), cycle);

        List<AgentStateOut.BeliefOut> beliefs = include.contains("beliefs") ? beliefs(entities, cycle, cut) : null;
        List<AgentStateOut.IntentionOut> intentions = null;
        List<AgentStateOut.IntendedMeansOut> intendedMeans = null;
        if (include.contains("intentions")) {
            intentions = new ArrayList<>();
            intendedMeans = new ArrayList<>();
            intentions(entities, cycle, cut, intentions, intendedMeans);
        }
        List<AgentStateOut.EventOut> events = include.contains("events") ? events(entities, cycle, cut) : null;
        return new AgentStateOut(in.agent(), cycle, clamped, oldest.cycle(), newest.cycle(), cut, beliefs, intentions, events, intendedMeans);
    }

    private static String change(Entity<?> e, int cycle, long cut) {
        if (e.getEnd() != null && e.getEnd().sequence() <= cut && e.getEnd().cycle() == cycle) return "removed";
        if (e.getStart().cycle() == cycle) return "new";
        return null;
    }

    private static boolean shown(Entity<?> e, int cycle, long cut) {
        if (e.getStart() == null || e.getStart().sequence() > cut) return false;
        if (e.getEnd() == null || e.getEnd().sequence() > cut) return true;
        return e.getEnd().cycle() == cycle;
    }

    private static long readCut(Entity<?> e, long cut) {
        if (e.getEnd() != null && e.getEnd().sequence() <= cut) return e.getEnd().sequence() - 1;
        return cut;
    }

    private List<AgentStateOut.BeliefOut> beliefs(List<Entity<?>> entities, int cycle, long cut) {
        List<Belief> list = new ArrayList<>();
        for (Entity<?> e : entities)
            if (e instanceof Belief b && shown(b, cycle, cut)) list.add(b);
        list.sort(Comparator.comparingLong(Belief::getOrderKey).thenComparingLong(Belief::getId));
        List<AgentStateOut.BeliefOut> r = new ArrayList<>();
        for (Belief b : list) {
            Set<Annotation> annotations = b.getAnnotations().elementsBefore(readCut(b, cut) + 1);
            List<String> texts = new ArrayList<>();
            for (Annotation a : annotations) texts.add(a.term());
            r.add(new AgentStateOut.BeliefOut(EntityIds.format(b), b.getContent(), texts, categories(b, annotations), change(b, cycle, cut)));
        }
        return r;
    }

    static List<String> categories(Belief b, Set<Annotation> annotations) {
        List<String> r = new ArrayList<>();
        if (b.isRule()) r.add("rule");
        for (Source s : Belief.sourcesOf(annotations)) {
            String c = switch (s.category()) {
                case SELF -> "mentalNote";
                case PERCEPT -> "environment";
                case AGENT -> "message";
                case OTHER -> "other";
            };
            if (!r.contains(c)) r.add(c);
        }
        return r;
    }

    private void intentions(List<Entity<?>> entities, int cycle, long cut, List<AgentStateOut.IntentionOut> out,
                            List<AgentStateOut.IntendedMeansOut> intendedMeans) {
        List<Intention> list = new ArrayList<>();
        for (Entity<?> e : entities)
            if (e instanceof Intention i && HistoryView.aliveAt(i, cut)) list.add(i);
        list.sort(Comparator.comparingLong(Intention::getId));
        for (Intention i : list) {
            Entity<?> waiting = i.getWaitingOn().before(cut + 1);
            Intention.State state = Intention.stateOf(i.getPlace().before(cut + 1), i.getSuspended().before(cut + 1), waiting);
            List<IntendedMeans> stack = i.stackAt(cut);
            List<AgentStateOut.LevelOut> levels = new ArrayList<>();
            boolean executed = false;
            for (int k = 0; k < stack.size(); k++) {
                IntendedMeans im = stack.get(k);
                levels.add(new AgentStateOut.LevelOut(EntityIds.format(im), k == 0 ? "intention" : "subgoal", im.label()));
                for (TimelineEntry<Integer> en : im.getCurrentDeed().entries()) {
                    if (en.occurrence().sequence() > cut) break;
                    if (en.occurrence().cycle() == cycle && en.occurrence().moment().step() == ReasoningStep.EXECUTE_INTENTION) executed = true;
                }
                intendedMeans.add(intendedMeans(im, i, cut));
            }
            out.add(new AgentStateOut.IntentionOut(EntityIds.format(i), state.name(), executed, HistoryView.ref(waiting), levels));
        }
    }

    private static AgentStateOut.IntendedMeansOut intendedMeans(IntendedMeans im, Intention owner, long cut) {
        Plan plan = im.getPlan();
        List<AgentStateOut.BindingOut> bindings = new ArrayList<>();
        for (Binding b : im.getBindings()) {
            if (b.sequence() > cut) break;
            Integer deed = im.getCurrentDeed().before(b.sequence() + 1);
            Integer line = null;
            if (plan != null) {
                if (deed != null && deed >= 0 && deed < plan.getBody().size()) line = plan.getBody().get(deed).line();
                else if (plan.getSource() != null) line = plan.getSource().line();
            }
            bindings.add(new AgentStateOut.BindingOut(b.name(), b.value(), line));
        }
        return new AgentStateOut.IntendedMeansOut(EntityIds.format(im), EntityIds.format(owner), EntityIds.format(plan),
                im.getCurrentDeed().before(cut + 1), bindings);
    }

    private List<AgentStateOut.EventOut> events(List<Entity<?>> entities, int cycle, long cut) {
        List<Event> all = new ArrayList<>();
        for (Entity<?> e : entities)
            if (e instanceof Event ev && shown(ev, cycle, cut)) all.add(ev);
        List<Event> ordered = new ArrayList<>();
        for (Event e : all)
            if (e.getPlace().before(cut + 1) == Event.Place.SELECTED && HistoryView.aliveAt(e, cut)) ordered.add(e);
        ordered.addAll(queue.eventQueue(all, cut));
        List<Event> rest = new ArrayList<>();
        for (Event e : all) if (!ordered.contains(e)) rest.add(e);
        rest.sort(Comparator.comparingLong(Event::getId));
        ordered.addAll(rest);
        List<AgentStateOut.EventOut> r = new ArrayList<>();
        for (Event e : ordered) {
            long rc = readCut(e, cut);
            r.add(new AgentStateOut.EventOut(EntityIds.format(e), e.getTrigger().before(rc + 1).text(), e.isAtomic(),
                    e.getPlace().before(rc + 1).name(), e.getState().before(rc + 1).name(), change(e, cycle, cut)));
        }
        return r;
    }
}
