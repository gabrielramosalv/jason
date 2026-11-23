package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.IntendedMeans;
import jason.asSemantics.Intention;
import jason.asSemantics.Message;
import jason.asSemantics.ReasoningListener;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.Plan;
import jason.asSyntax.PlanBody;
import jason.asSyntax.SourceInfo;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import jason.mind.api.core.domain.exception.history.InvariantViolationException;
import jason.mind.api.core.domain.model.entities.Action;
import jason.mind.api.core.domain.model.entities.ActionResult;
import jason.mind.api.core.domain.model.entities.Performative;
import jason.mind.api.core.domain.model.entities.SendMode;
import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.program.SourceFile;
import jason.mind.api.core.domain.model.program.SourceLocation;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.domain.model.trace.Verdict;
import jason.mind.api.core.ports.in.capture.CauseIn;
import jason.mind.api.core.ports.in.capture.EndAgentIn;
import jason.mind.api.core.ports.in.capture.EntityRef;
import jason.mind.api.core.ports.in.capture.EntitySpec;
import jason.mind.api.core.ports.in.capture.FinishCycleIn;
import jason.mind.api.core.ports.in.capture.LoadSourcesIn;
import jason.mind.api.core.ports.in.capture.RecordConsoleLineIn;
import jason.mind.api.core.ports.in.capture.RecordOccurrenceIn;
import jason.mind.api.core.ports.in.capture.RecordOccurrenceOut;
import jason.mind.api.core.ports.in.capture.RecordPlanSelectionIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JasonCaptureAdapter implements ReasoningListener {

    private static final Logger LOGGER = Logger.getLogger("Mind");
    private static final long SPACING = 1L << 20;

    public static final AtomicLong CAPTURE_ERRORS = new AtomicLong();

    public static final AtomicLong CAUSES_DROPPED = new AtomicLong();

    private static final jason.mind.api.core.domain.model.entities.Event.Place QUEUE = jason.mind.api.core.domain.model.entities.Event.Place.QUEUE;
    private static final jason.mind.api.core.domain.model.entities.Event.Place PENDING = jason.mind.api.core.domain.model.entities.Event.Place.PENDING;
    private static final jason.mind.api.core.domain.model.entities.Event.Place SELECTED = jason.mind.api.core.domain.model.entities.Event.Place.SELECTED;

    private final CapturePorts ports;
    private final MessageDirectory directory;
    private final SourceFiles sourceFiles;
    private final Agent ag;
    private final String agent;

    private boolean loaded;
    private boolean ended;
    private volatile int cycle;
    private volatile boolean inCycle;
    private volatile int lastFinishedCycle;
    private final int[] iteration = {-1, -1, -1};
    private final Map<ReasoningStep, Integer> ordinals = new HashMap<>();
    private final boolean[] active = new boolean[3];
    private ReasoningStep currentStep;
    private Moment stepMoment;
    private int gapOrdinal;

    private boolean findOpen;
    private Moment findMoment;
    private jason.asSemantics.Event findEvent;
    private PlanSelection.SearchMethod searchMethod;
    private List<RecordPlanSelectionIn.Evaluation> evaluations;

    private jason.asSemantics.Event seObject;
    private boolean seConsumed;

    private IntendedMeans execIM;
    private CauseIn execCause;

    private CauseIn stepCause;
    private long stepCauseId = -1;

    private final Map<Plan, TrackedPlan> plans = new IdentityHashMap<>();
    private final Map<Plan, PlanIndex> planIndexes = new IdentityHashMap<>();
    private final Map<Literal, TrackedBelief> beliefs = new IdentityHashMap<>();
    private final Map<jason.asSemantics.Event, TrackedEvent> events = new IdentityHashMap<>();
    private final Map<Integer, TrackedIntention> intentions = new HashMap<>();
    private final Map<IntendedMeans, TrackedIM> ims = new IdentityHashMap<>();
    private final Map<ActionExec, Long> actions = new IdentityHashMap<>();
    private final Map<Message, Long> mailbox = new IdentityHashMap<>();
    private int recorded;

    JasonCaptureAdapter(CapturePorts ports, MessageDirectory directory, SourceFiles sourceFiles, Agent ag, String agent) {
        this.ports = ports;
        this.directory = directory;
        this.sourceFiles = sourceFiles;
        this.ag = ag;
        this.agent = agent;
    }

    @Override
    public synchronized void cycleStarting(TransitionSystem ts, int cycleNumber) {
        try {
            cycleStartingCaptured(ts, cycleNumber);
        } catch (RuntimeException e) {
            error(e);
        }
    }

    private void cycleStartingCaptured(TransitionSystem ts, int cycleNumber) {
        if (ended) return;
        if (!loaded) initialLoad();
        cycle = cycleNumber;
        inCycle = true;
        iteration[0] = iteration[1] = iteration[2] = -1;
        active[0] = active[1] = active[2] = false;
        ordinals.clear();
        gapOrdinal = 0;
        gapDiff();
    }

    @Override
    public synchronized void cycleFinished(TransitionSystem ts, int cycleNumber) {
        try {
            cycleFinishedCaptured(ts, cycleNumber);
        } catch (RuntimeException e) {
            error(e);
        }
    }

    private void cycleFinishedCaptured(TransitionSystem ts, int cycleNumber) {
        if (ended) return;
        flushFind();
        gapDiff();
        inCycle = false;
        safe(() -> ports.finishCycle().execute(new FinishCycleIn(agent, cycleNumber, active[0], active[1], active[2])));
        lastFinishedCycle = cycleNumber;
    }

    @Override
    public synchronized void phaseStarting(TransitionSystem ts, Phase phase) {
        try {
            phaseStartingCaptured(ts, phase);
        } catch (RuntimeException e) {
            error(e);
        }
    }

    private void phaseStartingCaptured(TransitionSystem ts, Phase phase) {
        if (ended || !inCycle) return;
        flushFind();
        int p = phase.ordinal();
        iteration[p]++;
        for (ReasoningStep s : ReasoningStep.values())
            if (s.getPhase().ordinal() == p) ordinals.remove(s);
    }

    @Override
    public synchronized void stepStarting(TransitionSystem ts, Step step) {
        try {
            stepStartingCaptured(ts, step);
        } catch (RuntimeException e) {
            error(e);
        }
    }

    private void stepStartingCaptured(TransitionSystem ts, Step step) {
        if (ended || !inCycle) return;
        ReasoningStep rs = ReasoningStep.valueOf(step.name());
        Circumstance c = ts.getC();
        if (rs == ReasoningStep.FIND_OPTION && findOpen && c.getSelectedEvent() == findEvent) {
            currentStep = rs;
            return;
        }
        flushFind();
        gapDiff();
        stepMoment = nextMoment(rs);
        currentStep = rs;
        updateSelectedEvent();
        if (rs == ReasoningStep.FIND_OPTION) {
            findOpen = true;
            findMoment = stepMoment;
            findEvent = c.getSelectedEvent();
            searchMethod = null;
            evaluations = null;
        } else if (rs == ReasoningStep.EXECUTE_INTENTION) {
            prepareExecution(c);
        }
    }

    @Override
    public synchronized void stepFinished(TransitionSystem ts, Step step) {
        try {
            stepFinishedCaptured(ts, step);
        } catch (RuntimeException e) {
            error(e);
        }
    }

    private void stepFinishedCaptured(TransitionSystem ts, Step step) {
        if (ended || !inCycle) return;
        ReasoningStep rs = ReasoningStep.valueOf(step.name());
        if (rs == ReasoningStep.FIND_OPTION) return;
        Circumstance c = ts.getC();
        updateSelectedEvent();
        if (rs == ReasoningStep.ADD_INTENDED_MEANS && c.getSelectedEvent() == seObject) seConsumed = true;
        if (rs == ReasoningStep.EXECUTE_INTENTION) recordCurrentDeed(c);
        diff(stepMoment, rs);
        currentStep = null;
        execIM = null;
        execCause = null;
    }

    @Override
    public synchronized void planSearchStarting(TransitionSystem ts, jason.asSemantics.Event event, SearchMethod method) {
        try {
            planSearchStartingCaptured(ts, event, method);
        } catch (RuntimeException e) {
            error(e);
        }
    }

    private void planSearchStartingCaptured(TransitionSystem ts, jason.asSemantics.Event event, SearchMethod method) {
        if (ended) return;
        searchMethod = PlanSelection.SearchMethod.valueOf(method.name());
        evaluations = new ArrayList<>();
    }

    @Override
    public synchronized void planEvaluated(TransitionSystem ts, Plan plan, boolean relevant, boolean applicable, List<TermResult> terms) {
        try {
            planEvaluatedCaptured(ts, plan, relevant, applicable, terms);
        } catch (RuntimeException e) {
            error(e);
        }
    }

    private void planEvaluatedCaptured(TransitionSystem ts, Plan plan, boolean relevant, boolean applicable, List<TermResult> terms) {
        if (ended || evaluations == null || !relevant) return;
        TrackedPlan tp = plans.get(plan);
        if (tp == null || tp.id < 0) return;
        List<RecordPlanSelectionIn.Term> ts2 = new ArrayList<>();
        for (TermResult t : terms)
            ts2.add(new RecordPlanSelectionIn.Term(t.index, Verdict.of(t.attempts, t.successes),
                    t.bindings == null ? List.of() : t.bindings));
        evaluations.add(new RecordPlanSelectionIn.Evaluation(tp.id, ts2));
    }

    @Override
    public synchronized void messageSent(TransitionSystem ts, Message m) {
        try {
            messageSentCaptured(ts, m);
        } catch (RuntimeException e) {
            error(e);
        }
    }

    private void messageSentCaptured(TransitionSystem ts, Message m) {
        if (ended) return;
        Moment moment = currentStep != null ? (currentStep == ReasoningStep.FIND_OPTION ? findMoment : stepMoment) : gapMoment();
        CauseIn cause = currentStep == ReasoningStep.EXECUTE_INTENTION ? execCause : null;
        Long reply = directory.get(m.getInReplyTo(), m.getSender());
        EntitySpec.MessageSpec spec = new EntitySpec.MessageSpec(sendNumber(m.getMsgId()),
                m.getMsgId() != null && m.getMsgId().startsWith(Message.msgIdSyncAskPrefix) ? SendMode.SYNCHRONOUS : SendMode.ASYNCHRONOUS,
                reply, null, m.getReceiver(), new Performative(m.getIlForce()), String.valueOf(m.getPropCont()));
        long id = birth(moment, cause, spec);
        if (currentStep == null && id >= 0) gapOrdinal++;
        if (id >= 0) directory.put(m.getMsgId(), m.getReceiver(), id);
    }

    synchronized void end() {
        if (ended) return;
        ended = true;
        safe(() -> ports.endAgent().execute(new EndAgentIn(agent, lastFinishedCycle)));
    }

    void consoleLine(String text) {
        if (ended) return;
        int c = inCycle ? cycle : lastFinishedCycle + 1;
        safe(() -> ports.recordConsoleLine().execute(new RecordConsoleLineIn(agent, c, text)));
    }

    private void initialLoad() {
        loaded = true;
        List<SourceFile> files = new ArrayList<>();
        Set<String> paths = new LinkedHashSet<>();
        if (ag.getASLSrc() != null) paths.add(ag.getASLSrc());
        for (Plan p : ag.getPL().getPlans()) {
            SourceInfo si = p.getSrcInfo();
            if (si != null && si.getSrcFile() != null && !si.getSrcFile().isEmpty()) paths.add(si.getSrcFile());
        }
        for (String p : paths) {
            SourceFile f = sourceFiles.get(p);
            if (f != null) files.add(f);
        }
        safe(() -> ports.loadSources().execute(new LoadSourcesIn(agent, files)));
        diff(Moment.of(agent, 0, 0, ReasoningStep.INITIAL_LOAD, 1), ReasoningStep.INITIAL_LOAD);
        safe(() -> ports.finishCycle().execute(new FinishCycleIn(agent, 0, false, false, false)));
    }

    private Moment nextMoment(ReasoningStep step) {
        int ordinal = ordinals.merge(step, 1, Integer::sum);
        return Moment.of(agent, cycle, Math.max(0, iteration[step.getPhase().ordinal()]), step, ordinal);
    }

    private Moment gapMoment() {
        int c = inCycle ? cycle : lastFinishedCycle + 1;
        return Moment.of(agent, c, 0, ReasoningStep.OUT_OF_CYCLE, gapOrdinal + 1);
    }

    private void gapDiff() {
        if (!loaded) return;
        int before = recorded;
        diff(gapMoment(), ReasoningStep.OUT_OF_CYCLE);
        if (recorded > before) gapOrdinal++;
    }

    private void markActive(Moment m) {
        switch (m.step().getPhase()) {
            case SENSE -> active[0] = true;
            case DELIBERATE -> active[1] = true;
            case ACT -> active[2] = true;
            default -> { }
        }
    }

    private void updateSelectedEvent() {
        jason.asSemantics.Event se = ag.getTS().getC().getSelectedEvent();
        if (se != seObject) {
            seObject = se;
            seConsumed = false;
        }
    }

    private void flushFind() {
        if (!findOpen) return;
        findOpen = false;
        Moment m = findMoment;
        Circumstance c = ag.getTS().getC();
        TrackedEvent se = findEvent == null ? null : events.get(findEvent);
        if (evaluations != null && se != null && se.id >= 0) {
            RecordPlanSelectionIn in = new RecordPlanSelectionIn(agent, m, se.id, searchMethod, List.copyOf(evaluations));
            if (safe(() -> ports.recordPlanSelection().execute(in))) markActive(m);
        }
        evaluations = null;
        updateSelectedEvent();
        diff(m, ReasoningStep.FIND_OPTION);
        if (c.getSelectedOption() == null && findEvent != null && findEvent == seObject && !seConsumed && se != null && events.get(findEvent) == se) {
            seConsumed = true;
            events.remove(findEvent);
            if (se.id >= 0) {
                record(new RecordOccurrenceIn.Change(agent, m, null, se.id, "state",
                        jason.mind.api.core.domain.model.entities.Event.State.NO_APPLICABLE));
                record(new RecordOccurrenceIn.End(agent, m, null, se.id));
            }
            if (contains(c.getEvents(), findEvent)) {
                Trigger t = JasonTranslator.trigger(findEvent.getTrigger());
                long id = birth(m, se.id >= 0 ? CauseIn.by(se.id) : null, new EntitySpec.EventSpec(t, findEvent.isAtomic(), QUEUE));
                events.put(findEvent, new TrackedEvent(id, t, QUEUE, jason.mind.api.core.domain.model.entities.Event.State.PENDING));
            }
            diff(m, ReasoningStep.FIND_OPTION);
        }
        findEvent = null;
    }

    private static boolean contains(Iterable<?> it, Object o) {
        for (Object x : it) if (x == o) return true;
        return false;
    }

    private void prepareExecution(Circumstance c) {
        Intention si = c.getSelectedIntention();
        execIM = si == null ? null : si.peek();
        execCause = null;
        if (si == null || execIM == null) return;
        TrackedIntention ti = intentions.get(si.getId());
        if (ti == null || ti.id < 0) return;
        TrackedPlan tp = plans.get(execIM.getPlan());
        Integer deed = tp == null ? null : index(execIM.getPlan()).indexOf(execIM.getCurrentStep());
        execCause = tp == null || tp.id < 0 ? CauseIn.by(ti.id) : new CauseIn(ti.id, tp.id, deed);
    }

    private void recordCurrentDeed(Circumstance c) {
        PlanBody last = c.getLastDeed();
        if (last == null || execIM == null || execCause == null) return;
        TrackedIM tim = ims.get(execIM);
        if (tim == null || tim.id < 0) return;
        Integer deed = index(execIM.getPlan()).indexOf(last);
        if (deed == null) return;

        record(new RecordOccurrenceIn.Change(agent, stepMoment, CauseIn.by(execCause.byId()), tim.id, "currentDeed", deed));
    }

    private PlanIndex index(Plan p) {
        return planIndexes.computeIfAbsent(p, PlanIndex::new);
    }

    private void diff(Moment m, ReasoningStep step) {
        Circumstance c = ag.getTS().getC();
        updateSelectedEvent();

        List<Plan> curPlans = new ArrayList<>(ag.getPL().getPlans());
        List<Literal> curBeliefs = new ArrayList<>();
        for (Literal l : ag.getBB()) curBeliefs.add(l);

        Map<jason.asSemantics.Event, jason.mind.api.core.domain.model.entities.Event.Place> curEvents = new IdentityHashMap<>();
        List<jason.asSemantics.Event> eventOrder = new ArrayList<>();
        boolean seAlive = seObject != null && !seConsumed;
        if (seAlive) putEvent(curEvents, eventOrder, seObject, SELECTED);
        if (c.getAtomicEvent() != null) putEvent(curEvents, eventOrder, c.getAtomicEvent(), QUEUE);
        for (jason.asSemantics.Event e : c.getEvents()) putEvent(curEvents, eventOrder, e, QUEUE);
        for (jason.asSemantics.Event e : c.getPendingEvents().values()) putEvent(curEvents, eventOrder, e, PENDING);

        Set<ActionExec> curActions = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ActionExec> actionOrder = new ArrayList<>();
        if (c.getAction() != null && curActions.add(c.getAction())) actionOrder.add(c.getAction());
        for (ActionExec a : c.getPendingActions().values()) if (curActions.add(a)) actionOrder.add(a);
        for (ActionExec a : c.getFeedbackActions()) if (curActions.add(a)) actionOrder.add(a);

        Map<Intention, Object> containedIn = new IdentityHashMap<>();
        for (jason.asSemantics.Event e : eventOrder)
            if (e.getIntention() != null) containedIn.putIfAbsent(e.getIntention(), e);
        for (ActionExec a : actionOrder)
            if (a.getIntention() != null) containedIn.putIfAbsent(a.getIntention(), a);
        Set<Intention> pending = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.addAll(c.getPendingIntentions().values());
        Set<Intention> running = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Iterator<Intention> it = c.getRunningIntentionsPlusAtomic(); it.hasNext(); ) running.add(it.next());
        Intention selected = c.getSelectedIntention();

        Map<Integer, Intention> curIntentions = new java.util.LinkedHashMap<>();
        for (Intention i : running) addIntention(curIntentions, i);
        if (selected != null) addIntention(curIntentions, selected);
        for (Intention i : pending) addIntention(curIntentions, i);
        for (Intention i : containedIn.keySet()) addIntention(curIntentions, i);

        Set<Message> curMailbox = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Message> mailOrder = new ArrayList<>();
        for (Message msg : c.getMailBox()) if (curMailbox.add(msg)) mailOrder.add(msg);

        stepCause = null;
        stepCauseId = -1;
        switch (step) {
            case EXECUTE_INTENTION -> stepCause = execCause;
            case FIND_OPTION, ADD_INTENDED_MEANS -> {
                TrackedEvent te = seObject == null ? null : events.get(seObject);
                if (te != null && te.id >= 0) stepCause = CauseIn.by(te.id);
            }
            case PROCESS_ACTIONS -> {
                for (Map.Entry<ActionExec, Long> e : actions.entrySet())
                    if (!curActions.contains(e.getKey()) && e.getValue() >= 0) { stepCause = CauseIn.by(e.getValue()); break; }
            }
            case PROCESS_MESSAGES -> {
                for (Map.Entry<Message, Long> e : mailbox.entrySet())
                    if (!curMailbox.contains(e.getKey()) && e.getValue() >= 0) { stepCause = CauseIn.by(e.getValue()); break; }
            }
            default -> { }
        }
        if (stepCause != null) stepCauseId = stepCause.byId();

        diffPlans(m, curPlans);
        diffBeliefs(m, curBeliefs);
        for (jason.asSemantics.Event e : eventOrder) {
            if (events.containsKey(e)) continue;
            Trigger t = JasonTranslator.trigger(e.getTrigger());
            var place = curEvents.get(e);
            long id = birth(m, stepCause, new EntitySpec.EventSpec(t, e.isAtomic(), place));
            events.put(e, new TrackedEvent(id, t, place, jason.mind.api.core.domain.model.entities.Event.State.PENDING));
        }
        for (ActionExec a : actionOrder) {
            if (actions.containsKey(a)) continue;
            actions.put(a, birth(m, stepCause, new EntitySpec.ActionSpec(String.valueOf(a.getActionTerm()))));
        }
        for (Intention i : curIntentions.values()) {
            if (intentions.containsKey(i.getId())) continue;
            var place = placeOf(i, pending, running, selected, containedIn);
            long id = birth(m, stepCause, new EntitySpec.IntentionSpec(place));
            intentions.put(i.getId(), new TrackedIntention(id, place, Boolean.FALSE, null));
        }
        Set<IntendedMeans> curIMs = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Intention i : curIntentions.values()) {
            List<IntendedMeans> stack = new ArrayList<>();
            for (IntendedMeans im : i) stack.add(im);
            Collections.reverse(stack);
            TrackedIntention ti = intentions.get(i.getId());
            for (IntendedMeans im : stack) {
                curIMs.add(im);
                if (ims.containsKey(im)) continue;
                long planId = planIdOf(m, im.getPlan());
                long id = planId < 0 || ti.id < 0 ? -1 : birth(m, stepCause, new EntitySpec.IntendedMeansSpec(planId, ti.id));
                ims.put(im, new TrackedIM(id));
            }
        }
        for (jason.asSemantics.Event e : eventOrder) {
            TrackedEvent te = events.get(e);
            if (te.id < 0) continue;
            Trigger t = te.trigger.text().equals(textOf(e)) ? te.trigger : JasonTranslator.trigger(e.getTrigger());
            if (t != te.trigger) {
                change(m, te.id, "trigger", t);
                te.trigger = t;
            }
            var place = curEvents.get(e);
            if (place != te.place) {
                change(m, te.id, "place", place);
                te.place = place;
            }
            if (place == SELECTED && te.state != jason.mind.api.core.domain.model.entities.Event.State.SELECTED) {
                change(m, te.id, "state", jason.mind.api.core.domain.model.entities.Event.State.SELECTED);
                te.state = jason.mind.api.core.domain.model.entities.Event.State.SELECTED;
            }
        }
        for (Intention i : curIntentions.values()) {
            TrackedIntention ti = intentions.get(i.getId());
            if (ti.id < 0) continue;
            var place = placeOf(i, pending, running, selected, containedIn);
            if (place != ti.place) {
                change(m, ti.id, "place", place);
                ti.place = place;
            }
            Boolean suspended = i.isSuspended();
            if (!suspended.equals(ti.suspended)) {
                change(m, ti.id, "suspended", suspended);
                ti.suspended = suspended;
            }
            Object waiting = containedIn.get(i);
            if (waiting != ti.waitingOn) {
                Long wid = waiting instanceof jason.asSemantics.Event e ? idOf(events.get(e))
                        : waiting instanceof ActionExec a ? actions.get(a) : null;
                if (waiting == null || (wid != null && wid >= 0)) {
                    change(m, ti.id, "waitingOn", waiting == null ? null : new EntityRef(wid));
                    ti.waitingOn = waiting;
                }
            }
        }
        for (Message msg : mailOrder) {
            if (mailbox.containsKey(msg)) continue;
            Long id = directory.get(msg.getMsgId(), agent);
            if (id == null) {
                Long reply = directory.get(msg.getInReplyTo(), msg.getSender());
                id = birth(m, null, new EntitySpec.MessageSpec(sendNumber(msg.getMsgId()), SendMode.ASYNCHRONOUS, reply,
                        msg.getSender() == null ? "?" : msg.getSender(), agent, new Performative(msg.getIlForce()),
                        String.valueOf(msg.getPropCont())));
                if (id >= 0) directory.put(msg.getMsgId(), agent, id);
            }
            mailbox.put(msg, id);
            if (id >= 0) record(new RecordOccurrenceIn.Arrival(agent, m, id));
        }

        for (Iterator<Map.Entry<IntendedMeans, TrackedIM>> it = ims.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<IntendedMeans, TrackedIM> e = it.next();
            if (curIMs.contains(e.getKey())) continue;
            TrackedIM value = e.getValue();
            it.remove();
            if (value.id >= 0) end(m, value.id);
        }
        for (Iterator<Map.Entry<ActionExec, Long>> it = actions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ActionExec, Long> e = it.next();
            if (curActions.contains(e.getKey())) continue;
            ActionExec a = e.getKey();
            long id = e.getValue();
            it.remove();
            if (id < 0) continue;
            if (step == ReasoningStep.PROCESS_ACTIONS) {
                ActionResult r = a.getResult() ? new ActionResult(Action.Result.SUCCESS, null, null)
                        : new ActionResult(Action.Result.FAILURE, a.getFailureReason() == null ? null : a.getFailureReason().toString(), a.getFailureMsg());
                record(new RecordOccurrenceIn.Change(agent, m, null, id, "result", r));
            }
            end(m, id);
        }
        for (Iterator<Map.Entry<jason.asSemantics.Event, TrackedEvent>> it = events.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<jason.asSemantics.Event, TrackedEvent> e = it.next();
            if (curEvents.containsKey(e.getKey())) continue;
            TrackedEvent value = e.getValue();
            it.remove();
            if (value.id >= 0) end(m, value.id);
        }
        for (Iterator<Map.Entry<Integer, TrackedIntention>> it = intentions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, TrackedIntention> e = it.next();
            if (curIntentions.containsKey(e.getKey())) continue;
            TrackedIntention value = e.getValue();
            it.remove();
            if (value.id >= 0) end(m, value.id);
        }
        Set<Literal> curBeliefSet = Collections.newSetFromMap(new IdentityHashMap<>());
        curBeliefSet.addAll(curBeliefs);
        for (Iterator<Map.Entry<Literal, TrackedBelief>> it = beliefs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Literal, TrackedBelief> e = it.next();
            if (curBeliefSet.contains(e.getKey())) continue;
            TrackedBelief value = e.getValue();
            it.remove();
            if (value.id >= 0) end(m, value.id);
        }
        Set<Plan> curPlanSet = Collections.newSetFromMap(new IdentityHashMap<>());
        curPlanSet.addAll(curPlans);
        for (Iterator<Map.Entry<Plan, TrackedPlan>> it = plans.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Plan, TrackedPlan> e = it.next();
            if (curPlanSet.contains(e.getKey()) || e.getValue().detached) continue;
            Plan key = e.getKey();
            TrackedPlan value = e.getValue();
            it.remove();
            planIndexes.remove(key);
            if (value.id >= 0) end(m, value.id);
        }
        for (Iterator<Map.Entry<Message, Long>> it = mailbox.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Message, Long> e = it.next();
            if (curMailbox.contains(e.getKey())) continue;
            long id = e.getValue();
            it.remove();
            if (id >= 0) record(new RecordOccurrenceIn.End(agent, m, null, id));
        }

        for (IntendedMeans im : curIMs) {
            TrackedIM tim = ims.get(im);
            if (tim == null || tim.id < 0 || im.getUnif() == null) continue;
            Unifier u = im.getUnif();
            for (VarTerm v : u) {
                String name = v.toString();
                if (name.startsWith("_")) continue;
                Term value = u.get(v);
                if (value == null || value.isVar()) continue;
                String text = value.toString();
                if (!text.equals(tim.bindings.get(name))) {
                    tim.bindings.put(name, text);
                    record(new RecordOccurrenceIn.AddBinding(agent, m, tim.id, name, text));
                }
            }
        }
    }

    private static void putEvent(Map<jason.asSemantics.Event, jason.mind.api.core.domain.model.entities.Event.Place> map,
                                 List<jason.asSemantics.Event> order, jason.asSemantics.Event e,
                                 jason.mind.api.core.domain.model.entities.Event.Place place) {
        if (map.putIfAbsent(e, place) == null) order.add(e);
    }

    private static void addIntention(Map<Integer, Intention> map, Intention i) {
        if (i != null && !i.isFinished()) map.putIfAbsent(i.getId(), i);
    }

    private static jason.mind.api.core.domain.model.entities.Intention.Place placeOf(Intention i, Set<Intention> pending, Set<Intention> running,
                                                                                    Intention selected, Map<Intention, Object> containedIn) {
        if (pending.contains(i)) return jason.mind.api.core.domain.model.entities.Intention.Place.PENDING_INTENTIONS;
        if (containedIn.containsKey(i)) return jason.mind.api.core.domain.model.entities.Intention.Place.NONE;
        if (running.contains(i)) return jason.mind.api.core.domain.model.entities.Intention.Place.RUNNING;
        if (i == selected) return jason.mind.api.core.domain.model.entities.Intention.Place.SELECTED_INTENTION;
        return jason.mind.api.core.domain.model.entities.Intention.Place.NONE;
    }

    private static String textOf(jason.asSemantics.Event e) {
        jason.asSyntax.Trigger t = e.getTrigger();
        return t.getOperator().toString() + t.getType().toString() + JasonTranslator.withoutAnnotations(t.getLiteral());
    }

    private static Long idOf(TrackedEvent te) {
        return te == null ? null : te.id;
    }

    private void diffPlans(Moment m, List<Plan> current) {
        for (int k = 0; k < current.size(); k++) {
            Plan p = current.get(k);
            TrackedPlan tp = plans.get(p);
            List<Annotation> labelAnnots = p.getLabel() == null ? List.of() : JasonTranslator.annotations(p.getLabel());
            Set<Source> sources = new LinkedHashSet<>(JasonTranslator.sources(labelAnnots));
            if (tp == null) {
                long key = orderKey(current, k, plans);
                long id = birthPlan(m, p, key, labelAnnots, sources);
                plans.put(p, new TrackedPlan(id, key, sources));
                continue;
            }
            if (tp.id < 0 || tp.sources.equals(sources)) continue;
            for (Source s : sources)
                if (!tp.sources.contains(s)) record(new RecordOccurrenceIn.CollectionChange(agent, m, stepCause, tp.id, "sources", s, true));
            for (Source s : tp.sources)
                if (!sources.contains(s)) record(new RecordOccurrenceIn.CollectionChange(agent, m, stepCause, tp.id, "sources", s, false));
            tp.sources = sources;
        }
    }

    private long birthPlan(Moment m, Plan p, long key, List<Annotation> labelAnnots, Set<Source> sources) {
        List<Annotation> others = new ArrayList<>();
        for (Annotation a : labelAnnots) if (!a.isSource()) others.add(a);
        SourceInfo si = p.getSrcInfo();
        SourceLocation location = null;
        if (si != null) {
            SourceFile f = sourceFiles.get(si.getSrcFile());
            Integer line = f == null || si.getSrcLine() <= 0 ? null : si.getBeginSrcLine() > 0 ? si.getBeginSrcLine() : si.getSrcLine();
            location = new SourceLocation(f, line);
        }
        String label = p.getLabel() == null ? null : p.getLabel().getFunctor();
        return birth(m, stepCause, new EntitySpec.PlanSpec(label, key, JasonTranslator.trigger(p.getTrigger()),
                JasonTranslator.context(p.getContext()), index(p).deeds(), others, location, new ArrayList<>(sources)));
    }

    private long planIdOf(Moment m, Plan p) {
        TrackedPlan tp = plans.get(p);
        if (tp == null && p.getLabel() != null) {
            Plan lib = ag.getPL().get(p.getLabel());
            if (lib != null) {
                tp = plans.get(lib);
                if (tp != null) planIndexes.putIfAbsent(p, index(lib));
            }
        }
        if (tp == null) {
            List<Annotation> labelAnnots = p.getLabel() == null ? List.of() : JasonTranslator.annotations(p.getLabel());
            Set<Source> sources = new LinkedHashSet<>(JasonTranslator.sources(labelAnnots));
            long id = birthPlan(m, p, Long.MAX_VALUE / 2, labelAnnots, sources);
            tp = new TrackedPlan(id, Long.MAX_VALUE / 2, sources);
            tp.detached = true;
            plans.put(p, tp);
        }
        return tp.id;
    }

    private void diffBeliefs(Moment m, List<Literal> current) {
        for (int k = 0; k < current.size(); k++) {
            Literal l = current.get(k);
            TrackedBelief tb = beliefs.get(l);
            List<Annotation> annots = JasonTranslator.annotations(l);
            if (tb == null) {
                long key = orderKey(current, k, beliefs);
                long id = birth(m, stepCause, new EntitySpec.BeliefSpec(JasonTranslator.withoutAnnotations(l), l.isRule(), key, annots));
                beliefs.put(l, new TrackedBelief(id, key, new LinkedHashSet<>(annots)));
                continue;
            }
            if (tb.id < 0) continue;
            Set<Annotation> now = new LinkedHashSet<>(annots);
            if (now.equals(tb.annotations)) continue;
            for (Annotation a : now)
                if (!tb.annotations.contains(a)) record(new RecordOccurrenceIn.CollectionChange(agent, m, stepCause, tb.id, "annotations", a, true));
            for (Annotation a : tb.annotations)
                if (!now.contains(a)) record(new RecordOccurrenceIn.CollectionChange(agent, m, stepCause, tb.id, "annotations", a, false));
            tb.annotations = now;
        }
    }

    private static <K> long orderKey(List<K> order, int index, Map<K, ? extends Ordered> known) {
        Long prev = null, next = null;
        for (int j = index - 1; j >= 0 && prev == null; j--) {
            Ordered o = known.get(order.get(j));
            if (o != null) prev = o.orderKey();
        }
        for (int j = index + 1; j < order.size() && next == null; j++) {
            Ordered o = known.get(order.get(j));
            if (o != null) next = o.orderKey();
        }
        if (prev == null && next == null) return 0;
        if (prev == null) return next - SPACING;
        if (next == null) return prev + SPACING;
        return next - prev > 1 ? prev + (next - prev) / 2 : prev + 1;
    }

    private long birth(Moment m, CauseIn cause, EntitySpec spec) {
        RecordOccurrenceOut out = record(new RecordOccurrenceIn.Birth(agent, m, cause, spec));
        return out == null ? -1 : out.entityId();
    }

    private void change(Moment m, long id, String timeline, Object value) {
        record(new RecordOccurrenceIn.Change(agent, m, id == stepCauseId ? null : stepCause, id, timeline, value));
    }

    private void end(Moment m, long id) {
        record(new RecordOccurrenceIn.End(agent, m, id == stepCauseId ? null : stepCause, id));
    }

    private RecordOccurrenceOut record(RecordOccurrenceIn in) {
        try {
            return recorded(in, ports.recordOccurrence().execute(in));
        } catch (InvariantViolationException e) {
            RecordOccurrenceIn without = withoutCause(in);
            if (without != null && "INV-029".equals(e.getInvariant()) && e.getMessage() != null && e.getMessage().contains("cause.by")) {
                CAUSES_DROPPED.incrementAndGet();
                try {
                    return recorded(without, ports.recordOccurrence().execute(without));
                } catch (RuntimeException e2) {
                    error(e2);
                    return null;
                }
            }
            error(e);
            return null;
        } catch (RuntimeException e) {
            error(e);
            return null;
        }
    }

    private RecordOccurrenceOut recorded(RecordOccurrenceIn in, RecordOccurrenceOut out) {
        if (out.sequence() > 0) {
            recorded++;
            markActive(in.moment());
        }
        return out;
    }

    private static RecordOccurrenceIn withoutCause(RecordOccurrenceIn in) {
        return switch (in) {
            case RecordOccurrenceIn.Birth b when b.cause() != null -> new RecordOccurrenceIn.Birth(b.agent(), b.moment(), null, b.spec());
            case RecordOccurrenceIn.Change c when c.cause() != null -> new RecordOccurrenceIn.Change(c.agent(), c.moment(), null, c.entityId(), c.timeline(), c.value());
            case RecordOccurrenceIn.CollectionChange c when c.cause() != null -> new RecordOccurrenceIn.CollectionChange(c.agent(), c.moment(), null, c.entityId(), c.collection(), c.element(), c.added());
            case RecordOccurrenceIn.End e when e.cause() != null -> new RecordOccurrenceIn.End(e.agent(), e.moment(), null, e.entityId());
            default -> null;
        };
    }

    private boolean safe(Runnable r) {
        try {
            r.run();
            return true;
        } catch (RuntimeException e) {
            error(e);
            return false;
        }
    }

    private void error(RuntimeException e) {
        long n = CAPTURE_ERRORS.incrementAndGet();
        if (n <= 20 || Long.bitCount(n) == 1)
            LOGGER.log(Level.WARNING, "Mind: falha na captura de " + agent + " (" + n + " no total): " + e, n <= 3 ? e : null);
    }

    private static Integer sendNumber(String msgId) {
        if (msgId == null) return null;
        int i = msgId.length();
        while (i > 0 && Character.isDigit(msgId.charAt(i - 1))) i--;
        if (i == msgId.length()) return null;
        try {
            return Integer.parseInt(msgId.substring(i));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private interface Ordered {
        long orderKey();
    }

    private static final class TrackedPlan implements Ordered {
        final long id;
        final long key;
        Set<Source> sources;
        boolean detached;

        TrackedPlan(long id, long key, Set<Source> sources) {
            this.id = id;
            this.key = key;
            this.sources = sources;
        }

        public long orderKey() { return key; }
    }

    private static final class TrackedBelief implements Ordered {
        final long id;
        final long key;
        Set<Annotation> annotations;

        TrackedBelief(long id, long key, Set<Annotation> annotations) {
            this.id = id;
            this.key = key;
            this.annotations = annotations;
        }

        public long orderKey() { return key; }
    }

    private static final class TrackedEvent {
        final long id;
        Trigger trigger;
        jason.mind.api.core.domain.model.entities.Event.Place place;
        jason.mind.api.core.domain.model.entities.Event.State state;

        TrackedEvent(long id, Trigger trigger, jason.mind.api.core.domain.model.entities.Event.Place place,
                     jason.mind.api.core.domain.model.entities.Event.State state) {
            this.id = id;
            this.trigger = trigger;
            this.place = place;
            this.state = state;
        }
    }

    private static final class TrackedIntention {
        final long id;
        jason.mind.api.core.domain.model.entities.Intention.Place place;
        Boolean suspended;
        Object waitingOn;

        TrackedIntention(long id, jason.mind.api.core.domain.model.entities.Intention.Place place, Boolean suspended, Object waitingOn) {
            this.id = id;
            this.place = place;
            this.suspended = suspended;
            this.waitingOn = waitingOn;
        }
    }

    private static final class TrackedIM {
        final long id;
        final Map<String, String> bindings = new HashMap<>();

        TrackedIM(long id) {
            this.id = id;
        }
    }

    @Override
    public String toString() {
        return "JasonCaptureAdapter[" + agent + "]";
    }

    boolean isEnded() {
        return ended;
    }

    String agentName() {
        return agent;
    }
}
