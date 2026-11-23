package jason.mind.api.core.application.cycle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.application.history.CycleNotFoundException;
import jason.mind.api.core.application.history.CyclePrunedException;
import jason.mind.api.core.domain.model.entities.Binding;
import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.domain.model.trace.TermEvaluation;
import jason.mind.api.core.domain.service.reasoning.Candidate;
import jason.mind.api.core.domain.service.reasoning.CandidatesDomainService;
import jason.mind.api.core.domain.service.reasoning.QueueDomainService;
import jason.mind.api.core.domain.service.reasoning.StepCandidates;
import jason.mind.api.core.ports.in.cycle.CycleReasoningOut;
import jason.mind.api.core.ports.in.cycle.FindCycleReasoningIn;
import jason.mind.api.core.ports.in.cycle.FindCycleReasoningPort;
import jason.mind.api.core.ports.in.history.EntityRefOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

public class FindCycleReasoningUseCase implements FindCycleReasoningPort {

    private final HistoryQueryRepositoryPort historyQuery;
    private final CandidatesDomainService candidates;
    private final QueueDomainService queue;

    public FindCycleReasoningUseCase(HistoryQueryRepositoryPort historyQuery, CandidatesDomainService candidates, QueueDomainService queue) {
        this.historyQuery = historyQuery;
        this.candidates = candidates;
        this.queue = queue;
    }

    private record StepKey(int iteration, ReasoningStep step, int ordinal) {
    }

    @Override
    public CycleReasoningOut execute(FindCycleReasoningIn in) {
        Agent agent = HistoryView.agent(historyQuery, in.agent());
        CycleRecord newest = historyQuery.lastClosedCycle(in.agent()).orElse(null);
        if (newest == null || in.cycle() > newest.cycle() || in.cycle() < 0) throw new CycleNotFoundException(in.agent(), in.cycle());
        CycleRecord oldest = historyQuery.oldestCycle(in.agent()).orElse(newest);
        if (in.cycle() < oldest.cycle()) throw new CyclePrunedException(in.agent(), in.cycle());

        Map<StepKey, List<OccurrenceRecord>> byStep = new TreeMap<>(Comparator.comparingInt(StepKey::iteration)
                .thenComparing(StepKey::step).thenComparingInt(StepKey::ordinal));
        for (OccurrenceRecord r : historyQuery.occurrencesAt(in.agent(), in.cycle())) {
            Moment m = r.occurrence().moment();
            if (!m.agent().equals(in.agent())) continue;
            byStep.computeIfAbsent(new StepKey(m.iteration(), m.step(), m.ordinal()), k -> new ArrayList<>()).add(r);
        }
        Map<StepKey, PlanSelection> selections = new TreeMap<>(Comparator.comparingInt(StepKey::iteration)
                .thenComparing(StepKey::step).thenComparingInt(StepKey::ordinal));
        for (PlanSelection ps : historyQuery.planSelectionsAt(in.agent(), in.cycle())) {
            StepKey k = new StepKey(ps.moment().iteration(), ps.moment().step(), ps.moment().ordinal());
            selections.put(k, ps);
            byStep.computeIfAbsent(k, x -> new ArrayList<>());
        }

        List<Entity<?>> entities = historyQuery.entitiesAt(in.agent(), in.cycle());
        List<Event> events = new ArrayList<>();
        List<Intention> intentions = new ArrayList<>();
        List<Plan> plans = new ArrayList<>();
        for (Entity<?> e : entities) {
            if (e instanceof Event ev) events.add(ev);
            else if (e instanceof Intention i) intentions.add(i);
            else if (e instanceof Plan p) plans.add(p);
        }
        plans.sort(Comparator.comparingLong(Plan::getOrderKey));

        List<CycleReasoningOut.StepOut> steps = new ArrayList<>();
        List<Map.Entry<StepKey, List<OccurrenceRecord>>> entries = new ArrayList<>(byStep.entrySet());
        for (int idx = 0; idx < entries.size(); idx++) {
            StepKey key = entries.get(idx).getKey();
            List<OccurrenceRecord> records = entries.get(idx).getValue();
            PlanSelection selection = selections.get(key);
            long firstSeq = records.isEmpty() ? (selection == null ? newest.sequenceEnd() : selection.sequence())
                    : records.get(0).sequence();
            if (selection != null) firstSeq = Math.min(firstSeq, selection.sequence());
            long before = firstSeq - 1;

            StepCandidates sc = StepCandidates.empty();
            String searchMethod = null;
            switch (key.step()) {
                case SELECT_EVENT -> {
                    Event chosen = (Event) changedTo(records, "place", Event.Place.SELECTED);
                    sc = candidates.selectEvent(queue.eventQueue(events, before), chosen, agent.getConfig());
                }
                case SELECT_INTENTION -> {
                    Intention chosen = (Intention) changedTo(records, "place", Intention.Place.SELECTED_INTENTION);
                    sc = candidates.selectIntention(queue.intentionQueue(intentions, before), chosen, agent.getConfig());
                }
                case FIND_OPTION -> {
                    Event event = selection != null ? selection.event() : eventOf(records);
                    if (event != null) {
                        Plan chosen = chosenPlan(entries, idx, event);
                        Trigger t = event.getTrigger().before(before + 1);
                        List<Plan> alive = new ArrayList<>();
                        for (Plan p : plans) if (HistoryView.aliveAt(p, before)) alive.add(p);
                        sc = candidates.findOption(event, t == null ? event.getBirthTrigger() : t, alive, selection, chosen, agent.getConfig());
                    }
                    searchMethod = selection == null ? null : selection.searchMethod().name();
                }
                case EXECUTE_INTENTION -> {
                    List<Entity<?>> inputs = new ArrayList<>();
                    for (Intention i : intentions)
                        if (HistoryView.aliveAt(i, before) && i.getPlace().before(before + 1) == Intention.Place.SELECTED_INTENTION) inputs.add(i);
                    sc = new StepCandidates(inputs, null, List.of());
                }
                default -> {
                }
            }
            steps.add(step(key, searchMethod, sc, records));
        }
        return new CycleReasoningOut(in.agent(), in.cycle(), steps);
    }

    private static Entity<?> changedTo(List<OccurrenceRecord> records, String timeline, Object value) {
        for (OccurrenceRecord r : records)
            if (r.kind() == OccurrenceRecord.Kind.CHANGE && timeline.equals(r.timeline()) && value.equals(r.value())) return r.entity();
        return null;
    }

    private static Event eventOf(List<OccurrenceRecord> records) {
        for (OccurrenceRecord r : records)
            if (r.entity() instanceof Event e && r.kind() != OccurrenceRecord.Kind.START) return e;
        return null;
    }

    private static Plan chosenPlan(List<Map.Entry<StepKey, List<OccurrenceRecord>>> entries, int idx, Event event) {
        if (idx + 1 >= entries.size() || entries.get(idx + 1).getKey().step() != ReasoningStep.ADD_INTENDED_MEANS) return null;
        for (OccurrenceRecord r : entries.get(idx + 1).getValue())
            if (r.kind() == OccurrenceRecord.Kind.START && r.entity() instanceof IntendedMeans im
                    && r.occurrence().cause() != null && r.occurrence().cause().by() == event)
                return im.getPlan();
        return null;
    }

    private static CycleReasoningOut.StepOut step(StepKey key, String searchMethod, StepCandidates sc, List<OccurrenceRecord> records) {
        List<EntityRefOut> created = new ArrayList<>();
        List<CycleReasoningOut.ChangeOut> changed = new ArrayList<>();
        List<EntityRefOut> removed = new ArrayList<>();
        for (OccurrenceRecord r : records) {
            switch (r.kind()) {
                case START, ARRIVAL -> created.add(HistoryView.ref(r.entity()));
                case END -> removed.add(HistoryView.ref(r.entity()));
                case CHANGE -> changed.add(new CycleReasoningOut.ChangeOut(r.entity().typeName(), EntityIds.format(r.entity()),
                        r.entity().label(), r.timeline(), HistoryView.valueText(r.value())));
            }
        }
        List<EntityRefOut> inputs = new ArrayList<>();
        for (Entity<?> e : sc.inputs()) inputs.add(HistoryView.ref(e));
        List<CycleReasoningOut.CandidateOut> cands = new ArrayList<>();
        for (Candidate c : sc.candidates()) {
            List<CycleReasoningOut.TermOut> terms = null;
            if (key.step() == ReasoningStep.FIND_OPTION) {
                terms = new ArrayList<>();
                if (c.evaluation() != null)
                    for (TermEvaluation t : c.evaluation().context()) {
                        List<CycleReasoningOut.BindingOut> bs = new ArrayList<>();
                        for (Binding b : t.bindings()) bs.add(new CycleReasoningOut.BindingOut(b.name(), b.value()));
                        terms.add(new CycleReasoningOut.TermOut(t.index(), t.result().name(), bs));
                    }
            }
            Entity<?> e = c.entity();
            cands.add(new CycleReasoningOut.CandidateOut(e.typeName(), EntityIds.format(e), e.label(), c.rule(), c.chosen(), terms));
        }
        return new CycleReasoningOut.StepOut(key.step().name(), key.step().getPhase().name(), key.iteration(), key.ordinal(), searchMethod,
                inputs, HistoryView.ref(sc.chosen()), cands, created, changed, removed);
    }
}
