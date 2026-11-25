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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter that captures the reasoning of one Jason agent and records it through the capture ports.
 * <p>
 * Listens to every cycle, phase and step of the transition system and, after each step, compares the agent
 * circumstance with the entities already tracked, recording births, changes and ends of plans, beliefs, events,
 * actions, intentions, intended means and messages. Changes that happen outside a step are recorded in an
 * out-of-cycle moment. Every listener callback is synchronized and never propagates a runtime exception; failures are
 * counted in {@link #CAPTURE_ERRORS} and logged.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class JasonCaptureAdapter implements ReasoningListener {

    /** Total number of capture failures across all adapters. */
    public static final AtomicLong CAPTURE_ERRORS = new AtomicLong();

    /** Total number of occurrences recorded without their cause after the cause was rejected. */
    public static final AtomicLong CAUSES_DROPPED = new AtomicLong();

    /** Logger for capture failures. */
    private static final Logger LOGGER = Logger.getLogger("Mind");

    /** Distance between the order keys of entities appended before the first or after the last known entity. */
    private static final long SPACING = 1L << 20;

    /** Place of an event waiting in the event queue. */
    private static final jason.mind.api.core.domain.model.entities.Event.Place QUEUE
            = jason.mind.api.core.domain.model.entities.Event.Place.QUEUE;

    /** Place of an event waiting in the pending events. */
    private static final jason.mind.api.core.domain.model.entities.Event.Place PENDING
            = jason.mind.api.core.domain.model.entities.Event.Place.PENDING;

    /** Place of the selected event. */
    private static final jason.mind.api.core.domain.model.entities.Event.Place SELECTED
            = jason.mind.api.core.domain.model.entities.Event.Place.SELECTED;

    /** Capture ports that receive the recorded occurrences. */
    private final CapturePorts ports;

    /** Directory shared across agents that links message ids to recorded message entities. */
    private final MessageDirectory directory;

    /** Cache of source files shared across agents. */
    private final SourceFiles sourceFiles;

    /** Jason agent being captured. */
    private final Agent ag;

    /** Name of the captured agent. */
    private final String agent;

    /** Iteration of each phase in the current cycle, indexed by phase ordinal. */
    private final int[] iteration = {-1, -1, -1};

    /** Number of moments already created for each step in the current phase iteration. */
    private final Map<ReasoningStep, Integer> ordinals = new HashMap<>();

    /** Whether each phase recorded anything in the current cycle, indexed by phase ordinal. */
    private final boolean[] active = new boolean[3];

    /** Tracked plans, compared by identity. */
    private final Map<Plan, TrackedPlan> plans = new IdentityHashMap<>();

    /** Deed index of each plan, compared by identity. */
    private final Map<Plan, PlanIndex> planIndexes = new IdentityHashMap<>();

    /** Tracked beliefs, compared by identity. */
    private final Map<Literal, TrackedBelief> beliefs = new IdentityHashMap<>();

    /** Tracked events, compared by identity. */
    private final Map<jason.asSemantics.Event, TrackedEvent> events = new IdentityHashMap<>();

    /** Tracked intentions, indexed by Jason intention id. */
    private final Map<Integer, TrackedIntention> intentions = new HashMap<>();

    /** Tracked intended means, compared by identity. */
    private final Map<IntendedMeans, TrackedIM> ims = new IdentityHashMap<>();

    /** Entity id of each tracked action, compared by identity. */
    private final Map<ActionExec, Long> actions = new IdentityHashMap<>();

    /** Entity id of each tracked mailbox message, compared by identity. */
    private final Map<Message, Long> mailbox = new IdentityHashMap<>();

    /** Whether the initial load of the agent has been recorded. */
    private boolean loaded;

    /** Source files already registered for the agent. */
    private final Set<SourceFile> registeredSources = new HashSet<>();

    /** Whether the capture of the agent has ended. */
    @Getter(AccessLevel.PACKAGE)
    private volatile boolean ended;

    /** Time the capture spent in the cycle that is closing, in nanoseconds, set by the metering listener. */
    private volatile long captureNanos;

    /** Number of the current reasoning cycle. */
    private volatile int cycle;

    /** Whether a reasoning cycle is in progress. */
    private volatile boolean inCycle;

    /** Number of the last reasoning cycle that finished. */
    private volatile int lastFinishedCycle;

    /** Step in progress, or {@code null} outside a step. */
    private ReasoningStep currentStep;

    /** Moment of the step in progress. */
    private Moment stepMoment;

    /** Number of out-of-cycle moments already used in the current cycle. */
    private int gapOrdinal;

    /** Whether an option search is open and not yet recorded. */
    private boolean findOpen;

    /** Moment of the open option search. */
    private Moment findMoment;

    /** Event selected when the open option search started. */
    private jason.asSemantics.Event findEvent;

    /** Search method of the open option search. */
    private PlanSelection.SearchMethod searchMethod;

    /** Plan evaluations gathered by the open option search, or {@code null} when no search is gathering. */
    private List<RecordPlanSelectionIn.Evaluation> evaluations;

    /** Last observed selected event. */
    private jason.asSemantics.Event seObject;

    /** Whether the last observed selected event has already been consumed. */
    private boolean seConsumed;

    /** Intended means on top of the intention selected for execution. */
    private IntendedMeans execIM;

    /** Cause of the occurrences produced by the intention execution in progress. */
    private CauseIn execCause;

    /** Cause attached to the occurrences of the comparison in progress. */
    private CauseIn stepCause;

    /** Entity id of {@link #stepCause}, or {@code -1} when there is no cause. */
    private long stepCauseId = -1;

    /** Number of occurrences that produced a new sequence. */
    private int recorded;

    /**
     * Checks whether an iterable contains an object by identity.
     *
     * @param it elements to search
     * @param o  object to find
     * @return {@code true} when some element is the same instance as {@code o}
     */
    private static boolean contains(Iterable<?> it, Object o) {
        for (Object x : it) {
            if (x == o) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds an event with its place unless the event is already present.
     *
     * @param map   place of each present event
     * @param order present events in insertion order
     * @param e     event to add
     * @param place place of the event
     */
    private static void putEvent(
            Map<jason.asSemantics.Event, jason.mind.api.core.domain.model.entities.Event.Place> map,
            List<jason.asSemantics.Event> order, jason.asSemantics.Event e,
            jason.mind.api.core.domain.model.entities.Event.Place place) {
        if (map.putIfAbsent(e, place) == null) {
            order.add(e);
        }
    }

    /**
     * Adds an unfinished intention unless an intention with the same id is already present.
     *
     * @param map present intentions indexed by id
     * @param i   intention to add, ignored when {@code null} or finished
     */
    private static void addIntention(Map<Integer, Intention> map, Intention i) {
        if (i != null && !i.isFinished()) {
            map.putIfAbsent(i.getId(), i);
        }
    }

    /**
     * Determines the place of an intention.
     *
     * @param i           intention to place
     * @param pending     pending intentions
     * @param running     running intentions, including the atomic one
     * @param selected    selected intention, or {@code null}
     * @param containedIn event or action that holds each suspended intention
     * @return the place of the intention, checked in the order pending, held, running and selected
     */
    private static jason.mind.api.core.domain.model.entities.Intention.Place placeOf(Intention i,
                                                                                     Set<Intention> pending,
                                                                                     Set<Intention> running,
                                                                                     Intention selected,
                                                                                     Map<Intention, Object> containedIn) {
        if (pending.contains(i)) {
            return jason.mind.api.core.domain.model.entities.Intention.Place.PENDING_INTENTIONS;
        }
        if (containedIn.containsKey(i)) {
            return jason.mind.api.core.domain.model.entities.Intention.Place.NONE;
        }
        if (running.contains(i)) {
            return jason.mind.api.core.domain.model.entities.Intention.Place.RUNNING;
        }
        if (i == selected) {
            return jason.mind.api.core.domain.model.entities.Intention.Place.SELECTED_INTENTION;
        }
        return jason.mind.api.core.domain.model.entities.Intention.Place.NONE;
    }

    /**
     * Renders the trigger of an event in the same form as the text of a domain trigger.
     *
     * @param e event whose trigger is rendered
     * @return the operator, type and literal text of the trigger, without annotations
     */
    private static String textOf(jason.asSemantics.Event e) {
        jason.asSyntax.Trigger t = e.getTrigger();
        return t.getOperator().toString() + t.getType().toString() + JasonTranslator.withoutAnnotations(t.getLiteral());
    }

    /**
     * Returns the entity id of a tracked event.
     *
     * @param te tracked event, or {@code null}
     * @return the entity id, or {@code null} when the event is not tracked
     */
    private static Long idOf(TrackedEvent te) {
        return te == null ? null : te.id;
    }

    /**
     * Computes the order key of a new element from the nearest known neighbours.
     *
     * @param order elements in their current order
     * @param index position of the new element
     * @param known tracked elements with their order keys
     * @param <K>   type of the elements
     * @return a key between the neighbour keys, {@link #SPACING} away from the only neighbour, or {@code 0} when no
     * neighbour is known
     */
    private static <K> long orderKey(List<K> order, int index, Map<K, ? extends Ordered> known) {
        Long prev = null, next = null;
        for (int j = index - 1; j >= 0 && prev == null; j--) {
            Ordered o = known.get(order.get(j));
            if (o != null) {
                prev = o.orderKey();
            }
        }
        for (int j = index + 1; j < order.size() && next == null; j++) {
            Ordered o = known.get(order.get(j));
            if (o != null) {
                next = o.orderKey();
            }
        }
        if (prev == null && next == null) {
            return 0;
        }
        if (prev == null) {
            return next - SPACING;
        }
        if (next == null) {
            return prev + SPACING;
        }
        return next - prev > 1 ? prev + (next - prev) / 2 : prev + 1;
    }

    /**
     * Copies an occurrence without its cause.
     *
     * @param in occurrence to copy
     * @return the occurrence without cause, or {@code null} when it has no cause or cannot carry one
     */
    private static RecordOccurrenceIn withoutCause(RecordOccurrenceIn in) {
        return switch (in) {
            case RecordOccurrenceIn.Birth b when b.cause() != null -> new RecordOccurrenceIn.Birth(b.agent(),
                    b.moment(), null, b.spec());
            case RecordOccurrenceIn.Change c when c.cause() != null -> new RecordOccurrenceIn.Change(c.agent(),
                    c.moment(), null, c.entityId(), c.timeline(), c.value());
            case RecordOccurrenceIn.CollectionChange c when c.cause() != null ->
                    new RecordOccurrenceIn.CollectionChange(c.agent(), c.moment(), null, c.entityId(), c.collection(),
                            c.element(), c.added());
            case RecordOccurrenceIn.End e when e.cause() != null -> new RecordOccurrenceIn.End(e.agent(), e.moment(),
                    null, e.entityId());
            default -> null;
        };
    }

    /**
     * Extracts the send number from the trailing digits of a message id.
     *
     * @param msgId Jason id of the message
     * @return the number formed by the trailing digits, or {@code null} when there are none or they overflow
     */
    private static Integer sendNumber(String msgId) {
        if (msgId == null) {
            return null;
        }
        int i = msgId.length();
        while (i > 0 && Character.isDigit(msgId.charAt(i - 1))) {
            i--;
        }
        if (i == msgId.length()) {
            return null;
        }
        try {
            return Integer.parseInt(msgId.substring(i));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Ends the capture of the agent; later calls do nothing.
     */
    synchronized void end() {
        if (this.ended) {
            return;
        }
        this.ended = true;
        this.safe(() -> this.ports.endAgent().execute(new EndAgentIn(this.agent, this.lastFinishedCycle)));
    }

    /**
     * Records one line of console output in the current cycle, or in the next one when no cycle is in progress.
     *
     * @param text formatted console line
     */
    void recordConsoleLine(String text) {
        if (this.ended) {
            return;
        }
        int c = this.inCycle ? this.cycle : this.lastFinishedCycle + 1;
        this.safe(() -> this.ports.recordConsoleLine().execute(new RecordConsoleLineIn(this.agent, c, text)));
    }

    /**
     * Returns the name of the captured agent.
     *
     * @return the agent name
     */
    String agentName() {
        return this.agent;
    }

    /**
     * Opens a reasoning cycle, recording the initial load first when needed and any change made since the last step.
     *
     * @param ts          transition system of the agent
     * @param cycleNumber number of the cycle that starts
     */
    private void captureCycleStarting(TransitionSystem ts, int cycleNumber) {
        if (this.ended) {
            return;
        }
        if (!this.loaded) {
            this.loadInitialState();
        }
        this.cycle = cycleNumber;
        this.inCycle = true;
        this.iteration[0] = this.iteration[1] = this.iteration[2] = -1;
        this.active[0] = this.active[1] = this.active[2] = false;
        this.ordinals.clear();
        this.gapOrdinal = 0;
        this.captureGapDiff();
    }

    /**
     * Sets the time the capture spent in the cycle that is about to close.
     *
     * @param nanos time in nanoseconds
     */
    void captureNanos(long nanos) {
        this.captureNanos = nanos;
    }

    /**
     * Closes a reasoning cycle, recording the pending option search and remaining changes before finishing it.
     *
     * @param ts          transition system of the agent
     * @param cycleNumber number of the cycle that finished
     */
    private void captureCycleFinished(TransitionSystem ts, int cycleNumber) {
        if (this.ended) {
            return;
        }
        this.flushFind();
        this.captureGapDiff();
        this.inCycle = false;
        this.safe(() -> this.ports.finishCycle()
                .execute(new FinishCycleIn(this.agent, cycleNumber, this.active[0], this.active[1], this.active[2],
                        this.captureNanos)));
        this.lastFinishedCycle = cycleNumber;
    }

    /**
     * Starts a new iteration of a phase and resets the step ordinals of that phase.
     *
     * @param ts    transition system of the agent
     * @param phase phase that starts
     */
    private void capturePhaseStarting(TransitionSystem ts, Phase phase) {
        if (this.ended || !this.inCycle) {
            return;
        }
        this.flushFind();
        int p = phase.ordinal();
        this.iteration[p]++;
        for (ReasoningStep s : ReasoningStep.values()) {
            if (s.getPhase().ordinal() == p) {
                this.ordinals.remove(s);
            }
        }
    }

    /**
     * Opens the moment of a step.
     * <p>
     * A repeated option search over the same selected event continues the open search instead of opening a new moment.
     *
     * @param ts   transition system of the agent
     * @param step step that starts
     */
    private void captureStepStarting(TransitionSystem ts, Step step) {
        if (this.ended || !this.inCycle) {
            return;
        }
        ReasoningStep rs = ReasoningStep.valueOf(step.name());
        Circumstance c = ts.getC();
        if (rs == ReasoningStep.FIND_OPTION && this.findOpen && c.getSelectedEvent() == this.findEvent) {
            this.currentStep = rs;
            return;
        }
        this.flushFind();
        this.captureGapDiff();
        this.stepMoment = this.nextMoment(rs);
        this.currentStep = rs;
        this.updateSelectedEvent();
        if (rs == ReasoningStep.FIND_OPTION) {
            this.findOpen = true;
            this.findMoment = this.stepMoment;
            this.findEvent = c.getSelectedEvent();
            this.searchMethod = null;
            this.evaluations = null;
        } else if (rs == ReasoningStep.EXECUTE_INTENTION) {
            this.prepareExecution(c);
        }
    }

    /**
     * Records the changes made by a step; option searches are recorded later, when the search is flushed.
     *
     * @param ts   transition system of the agent
     * @param step step that finished
     */
    private void captureStepFinished(TransitionSystem ts, Step step) {
        if (this.ended || !this.inCycle) {
            return;
        }
        ReasoningStep rs = ReasoningStep.valueOf(step.name());
        if (rs == ReasoningStep.FIND_OPTION) {
            return;
        }
        Circumstance c = ts.getC();
        this.updateSelectedEvent();
        if (rs == ReasoningStep.ADD_INTENDED_MEANS && c.getSelectedEvent() == this.seObject) {
            this.seConsumed = true;
        }
        if (rs == ReasoningStep.EXECUTE_INTENTION) {
            this.recordCurrentDeed(c);
        }
        this.captureDiff(this.stepMoment, rs);
        this.currentStep = null;
        this.execIM = null;
        this.execCause = null;
    }

    /**
     * Starts gathering the plan evaluations of an option search.
     *
     * @param ts     transition system of the agent
     * @param event  event whose options are searched
     * @param method method used to search the relevant plans
     */
    private void capturePlanSearchStarting(TransitionSystem ts, jason.asSemantics.Event event, SearchMethod method) {
        if (this.ended) {
            return;
        }
        this.searchMethod = PlanSelection.SearchMethod.valueOf(method.name());
        this.evaluations = new ArrayList<>();
    }

    /**
     * Adds the evaluation of a relevant, recorded plan to the open option search.
     *
     * @param ts         transition system of the agent
     * @param plan       evaluated plan
     * @param relevant   whether the plan is relevant for the event
     * @param applicable whether the plan context holds
     * @param terms      result of each evaluated context term
     */
    private void capturePlanEvaluated(TransitionSystem ts, Plan plan, boolean relevant, boolean applicable,
                                       List<TermResult> terms) {
        if (this.ended || this.evaluations == null || !relevant) {
            return;
        }
        TrackedPlan tp = this.plans.get(plan);
        if (tp == null || tp.id < 0) {
            return;
        }
        List<RecordPlanSelectionIn.Term> ts2 = new ArrayList<>();
        for (TermResult t : terms) {
            ts2.add(new RecordPlanSelectionIn.Term(t.index, Verdict.of(t.attempts, t.successes),
                    t.bindings == null ? List.of() : t.bindings));
        }
        this.evaluations.add(new RecordPlanSelectionIn.Evaluation(tp.id, ts2));
    }

    /**
     * Records the birth of a sent message and registers it in the message directory.
     *
     * @param ts transition system of the agent
     * @param m  sent message
     */
    private void captureMessageSent(TransitionSystem ts, Message m) {
        if (this.ended) {
            return;
        }
        Moment moment = this.currentStep != null ?
                        (this.currentStep == ReasoningStep.FIND_OPTION ? this.findMoment : this.stepMoment) :
                        this.gapMoment();
        CauseIn cause = this.currentStep == ReasoningStep.EXECUTE_INTENTION ? this.execCause : null;
        Long reply = this.directory.get(m.getInReplyTo(), m.getSender());
        EntitySpec.MessageSpec spec = new EntitySpec.MessageSpec(sendNumber(m.getMsgId()),
                m.getMsgId() != null && m.getMsgId().startsWith(Message.msgIdSyncAskPrefix) ? SendMode.SYNCHRONOUS :
                SendMode.ASYNCHRONOUS, reply, null, m.getReceiver(), new Performative(m.getIlForce()),
                String.valueOf(m.getPropCont()));
        long id = this.recordEntityBirth(moment, cause, spec);
        if (this.currentStep == null && id >= 0) {
            this.gapOrdinal++;
        }
        if (id >= 0) {
            this.directory.put(m.getMsgId(), m.getReceiver(), id);
        }
    }

    /**
     * Records the source files and the initial state of the agent as cycle zero.
     */
    private void loadInitialState() {
        this.loaded = true;
        List<SourceFile> files = new ArrayList<>();
        Set<String> paths = new LinkedHashSet<>();
        if (this.ag.getASLSrc() != null) {
            paths.add(this.ag.getASLSrc());
        }
        for (Plan p : this.ag.getPL().getPlans()) {
            SourceInfo si = p.getSrcInfo();
            if (si != null && si.getSrcFile() != null && !si.getSrcFile().isEmpty()) {
                paths.add(si.getSrcFile());
            }
        }
        for (String p : paths) {
            SourceFile f = this.sourceFiles.get(p);
            if (f != null) {
                files.add(f);
            }
        }
        this.registeredSources.addAll(files);
        this.safe(() -> this.ports.loadSources().execute(new LoadSourcesIn(this.agent, files)));
        this.captureDiff(Moment.of(this.agent, 0, 0, ReasoningStep.INITIAL_LOAD, 1), ReasoningStep.INITIAL_LOAD);
        this.safe(() -> this.ports.finishCycle().execute(new FinishCycleIn(this.agent, 0, false, false, false, 0)));
    }

    /**
     * Registers a source file of the agent the first time it is cited by a plan.
     *
     * @param file source file cited by a plan
     */
    private void registerSource(SourceFile file) {
        if (this.registeredSources.add(file)) {
            this.safe(() -> this.ports.loadSources().execute(new LoadSourcesIn(this.agent, List.of(file))));
        }
    }

    /**
     * Creates the next moment of a step in the current phase iteration.
     *
     * @param step step that gets the moment
     * @return the moment with the next ordinal of the step
     */
    private Moment nextMoment(ReasoningStep step) {
        int ordinal = this.ordinals.merge(step, 1, Integer::sum);
        return Moment.of(this.agent, this.cycle, Math.max(0, this.iteration[step.getPhase().ordinal()]), step, ordinal);
    }

    /**
     * Creates the out-of-cycle moment for changes made outside a step.
     *
     * @return the out-of-cycle moment of the current cycle, or of the next one when no cycle is in progress
     */
    private Moment gapMoment() {
        int c = this.inCycle ? this.cycle : this.lastFinishedCycle + 1;
        return Moment.of(this.agent, c, 0, ReasoningStep.OUT_OF_CYCLE, this.gapOrdinal + 1);
    }

    /**
     * Records the changes made outside a step, moving to the next out-of-cycle moment when anything was recorded.
     */
    private void captureGapDiff() {
        if (!this.loaded) {
            return;
        }
        int before = this.recorded;
        this.captureDiff(this.gapMoment(), ReasoningStep.OUT_OF_CYCLE);
        if (this.recorded > before) {
            this.gapOrdinal++;
        }
    }

    /**
     * Marks the phase of a moment as active in the current cycle.
     *
     * @param m moment whose phase recorded something
     */
    private void markActive(Moment m) {
        switch (m.step().getPhase()) {
            case SENSE -> this.active[0] = true;
            case DELIBERATE -> this.active[1] = true;
            case ACT -> this.active[2] = true;
            default -> {
            }
        }
    }

    /**
     * Refreshes the last observed selected event, resetting its consumed flag when it changed.
     */
    private void updateSelectedEvent() {
        jason.asSemantics.Event se = this.ag.getTS().getC().getSelectedEvent();
        if (se != this.seObject) {
            this.seObject = se;
            this.seConsumed = false;
        }
    }

    /**
     * Records the open option search, if any.
     * <p>
     * Records the plan selection and the changes of the search. When no option was selected, the event ends with no
     * applicable plan and, if it was put back in the queue, a new queued event is born.
     */
    private void flushFind() {
        if (!this.findOpen) {
            return;
        }
        this.findOpen = false;
        Moment m = this.findMoment;
        Circumstance c = this.ag.getTS().getC();
        TrackedEvent se = this.findEvent == null ? null : this.events.get(this.findEvent);
        if (this.evaluations != null && se != null && se.id >= 0) {
            RecordPlanSelectionIn in = new RecordPlanSelectionIn(this.agent, m, se.id, this.searchMethod,
                    List.copyOf(this.evaluations));
            if (this.safe(() -> this.ports.recordPlanSelection().execute(in))) {
                this.markActive(m);
            }
        }
        this.evaluations = null;
        this.updateSelectedEvent();
        this.captureDiff(m, ReasoningStep.FIND_OPTION);
        if (c.getSelectedOption() == null && this.findEvent != null && this.findEvent == this.seObject
                && !this.seConsumed && se != null && this.events.get(this.findEvent) == se) {
            this.seConsumed = true;
            this.events.remove(this.findEvent);
            if (se.id >= 0) {
                this.record(new RecordOccurrenceIn.Change(this.agent, m, null, se.id, "state",
                        jason.mind.api.core.domain.model.entities.Event.State.NO_APPLICABLE));
                this.record(new RecordOccurrenceIn.End(this.agent, m, null, se.id));
            }
            if (contains(c.getEvents(), this.findEvent)) {
                Trigger t = JasonTranslator.trigger(this.findEvent.getTrigger());
                long id = this.recordEntityBirth(m, se.id >= 0 ? CauseIn.by(se.id) : null,
                        new EntitySpec.EventSpec(t, this.findEvent.isAtomic(), QUEUE));
                this.events.put(this.findEvent,
                        new TrackedEvent(id, t, QUEUE, jason.mind.api.core.domain.model.entities.Event.State.PENDING));
            }
            this.captureDiff(m, ReasoningStep.FIND_OPTION);
        }
        this.findEvent = null;
    }

    /**
     * Resolves the intended means and the cause of the intention selected for execution.
     *
     * @param c circumstance of the agent
     */
    private void prepareExecution(Circumstance c) {
        Intention si = c.getSelectedIntention();
        this.execIM = si == null ? null : si.peek();
        this.execCause = null;
        if (si == null || this.execIM == null) {
            return;
        }
        TrackedIntention ti = this.intentions.get(si.getId());
        if (ti == null || ti.id < 0) {
            return;
        }
        TrackedPlan tp = this.plans.get(this.execIM.getPlan());
        Integer deed = tp == null ? null : this.index(this.execIM.getPlan()).indexOf(this.execIM.getCurrentStep());
        // quem age é o objetivo do topo da pilha (o IntendedMeans); a intenção só quando ele não foi rastreado
        TrackedIM executing = this.ims.get(this.execIM);
        long doer = executing != null && executing.id >= 0 ? executing.id : ti.id;
        this.execCause = tp == null || tp.id < 0 ? CauseIn.by(doer) : new CauseIn(doer, tp.id, deed);
    }

    /**
     * Records the deed just executed as the current deed of the executing intended means.
     *
     * @param c circumstance of the agent
     */
    private void recordCurrentDeed(Circumstance c) {
        PlanBody last = c.getLastDeed();
        if (last == null || this.execIM == null || this.execCause == null) {
            return;
        }
        TrackedIM tim = this.ims.get(this.execIM);
        if (tim == null || tim.id < 0) {
            return;
        }
        Integer deed = this.index(this.execIM.getPlan()).indexOf(last);
        if (deed == null) {
            return;
        }

        this.record(
                new RecordOccurrenceIn.Change(this.agent, this.stepMoment, null, tim.id, "currentDeed", deed));
    }

    /**
     * Returns the deed index of a plan, building it on first access.
     *
     * @param p plan to index
     * @return the deed index of the plan
     */
    private PlanIndex index(Plan p) {
        return this.planIndexes.computeIfAbsent(p, PlanIndex::new);
    }

    /**
     * Compares the agent circumstance with the tracked entities and records every difference in a moment.
     * <p>
     * Births are recorded first, then changes, then ends and finally new variable bindings. The cause of the
     * occurrences depends on the step: the executing intention, the selected event, the processed action or the
     * processed message.
     *
     * @param m    moment of the recorded occurrences
     * @param step step whose changes are recorded
     */
    private void captureDiff(Moment m, ReasoningStep step) {
        Circumstance c = this.ag.getTS().getC();
        this.updateSelectedEvent();

        List<Plan> curPlans = new ArrayList<>(this.ag.getPL().getPlans());
        List<Literal> curBeliefs = new ArrayList<>();
        for (Literal l : this.ag.getBB()) {
            curBeliefs.add(l);
        }

        Map<jason.asSemantics.Event, jason.mind.api.core.domain.model.entities.Event.Place> curEvents
                = new IdentityHashMap<>();
        List<jason.asSemantics.Event> eventOrder = new ArrayList<>();
        boolean seAlive = this.seObject != null && !this.seConsumed;
        if (seAlive) {
            putEvent(curEvents, eventOrder, this.seObject, SELECTED);
        }
        if (c.getAtomicEvent() != null) {
            putEvent(curEvents, eventOrder, c.getAtomicEvent(), QUEUE);
        }
        for (jason.asSemantics.Event e : c.getEvents()) {
            putEvent(curEvents, eventOrder, e, QUEUE);
        }
        for (jason.asSemantics.Event e : c.getPendingEvents().values()) {
            putEvent(curEvents, eventOrder, e, PENDING);
        }

        Set<ActionExec> curActions = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ActionExec> actionOrder = new ArrayList<>();
        if (c.getAction() != null && curActions.add(c.getAction())) {
            actionOrder.add(c.getAction());
        }
        for (ActionExec a : c.getPendingActions().values()) {
            if (curActions.add(a)) {
                actionOrder.add(a);
            }
        }
        for (ActionExec a : c.getFeedbackActions()) {
            if (curActions.add(a)) {
                actionOrder.add(a);
            }
        }

        Map<Intention, Object> containedIn = new IdentityHashMap<>();
        for (jason.asSemantics.Event e : eventOrder) {
            if (e.getIntention() != null) {
                containedIn.putIfAbsent(e.getIntention(), e);
            }
        }
        for (ActionExec a : actionOrder) {
            if (a.getIntention() != null) {
                containedIn.putIfAbsent(a.getIntention(), a);
            }
        }
        Set<Intention> pending = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.addAll(c.getPendingIntentions().values());
        Set<Intention> running = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Iterator<Intention> it = c.getRunningIntentionsPlusAtomic(); it.hasNext(); ) {
            running.add(it.next());
        }
        Intention selected = c.getSelectedIntention();

        Map<Integer, Intention> curIntentions = new LinkedHashMap<>();
        for (Intention i : running) {
            addIntention(curIntentions, i);
        }
        if (selected != null) {
            addIntention(curIntentions, selected);
        }
        for (Intention i : pending) {
            addIntention(curIntentions, i);
        }
        for (Intention i : containedIn.keySet()) {
            addIntention(curIntentions, i);
        }

        Set<Message> curMailbox = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Message> mailOrder = new ArrayList<>();
        for (Message msg : c.getMailBox()) {
            if (curMailbox.add(msg)) {
                mailOrder.add(msg);
            }
        }

        this.stepCause = null;
        this.stepCauseId = -1;
        switch (step) {
            case EXECUTE_INTENTION -> this.stepCause = this.execCause;
            case FIND_OPTION, ADD_INTENDED_MEANS -> {
                TrackedEvent te = this.seObject == null ? null : this.events.get(this.seObject);
                if (te != null && te.id >= 0) {
                    this.stepCause = CauseIn.by(te.id);
                }
            }
            case PROCESS_ACTIONS -> {
                for (Map.Entry<ActionExec, Long> e : this.actions.entrySet()) {
                    if (!curActions.contains(e.getKey()) && e.getValue() >= 0) {
                        this.stepCause = CauseIn.by(e.getValue());
                        break;
                    }
                }
            }
            case PROCESS_MESSAGES -> {
                for (Map.Entry<Message, Long> e : this.mailbox.entrySet()) {
                    if (!curMailbox.contains(e.getKey()) && e.getValue() >= 0) {
                        this.stepCause = CauseIn.by(e.getValue());
                        break;
                    }
                }
            }
            default -> {
            }
        }
        if (this.stepCause != null) {
            this.stepCauseId = this.stepCause.byId();
        }

        this.capturePlanChanges(m, curPlans);
        this.captureBeliefChanges(m, curBeliefs);
        for (jason.asSemantics.Event e : eventOrder) {
            if (this.events.containsKey(e)) {
                continue;
            }
            Trigger t = JasonTranslator.trigger(e.getTrigger());
            var place = curEvents.get(e);
            long id = this.recordEntityBirth(m, this.stepCause, new EntitySpec.EventSpec(t, e.isAtomic(), place));
            this.events.put(e,
                    new TrackedEvent(id, t, place, jason.mind.api.core.domain.model.entities.Event.State.PENDING));
        }
        for (ActionExec a : actionOrder) {
            if (this.actions.containsKey(a)) {
                continue;
            }
            this.actions.put(a,
                    this.recordEntityBirth(m, this.stepCause, new EntitySpec.ActionSpec(String.valueOf(a.getActionTerm()))));
        }
        for (Intention i : curIntentions.values()) {
            if (this.intentions.containsKey(i.getId())) {
                continue;
            }
            var place = placeOf(i, pending, running, selected, containedIn);
            long id = this.recordEntityBirth(m, this.stepCause, new EntitySpec.IntentionSpec(place));
            this.intentions.put(i.getId(), new TrackedIntention(id, place, Boolean.FALSE, null));
        }
        Set<IntendedMeans> curIMs = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Intention i : curIntentions.values()) {
            List<IntendedMeans> stack = new ArrayList<>();
            for (IntendedMeans im : i) {
                stack.add(im);
            }
            Collections.reverse(stack);
            TrackedIntention ti = this.intentions.get(i.getId());
            for (IntendedMeans im : stack) {
                curIMs.add(im);
                if (this.ims.containsKey(im)) {
                    continue;
                }
                long planId = this.planIdOf(m, im.getPlan());
                long id = planId < 0 || ti.id < 0 ? -1 : this.recordEntityBirth(m, this.stepCause,
                        new EntitySpec.IntendedMeansSpec(planId, ti.id));
                this.ims.put(im, new TrackedIM(id));
            }
        }
        for (jason.asSemantics.Event e : eventOrder) {
            TrackedEvent te = this.events.get(e);
            if (te.id < 0) {
                continue;
            }
            Trigger t = te.trigger.text().equals(textOf(e)) ? te.trigger : JasonTranslator.trigger(e.getTrigger());
            if (t != te.trigger) {
                this.recordEntityChange(m, te.id, "trigger", t);
                te.trigger = t;
            }
            var place = curEvents.get(e);
            if (place != te.place) {
                this.recordEntityChange(m, te.id, "place", place);
                te.place = place;
            }
            if (place == SELECTED && te.state != jason.mind.api.core.domain.model.entities.Event.State.SELECTED) {
                this.recordEntityChange(m, te.id, "state", jason.mind.api.core.domain.model.entities.Event.State.SELECTED);
                te.state = jason.mind.api.core.domain.model.entities.Event.State.SELECTED;
            }
        }
        for (Intention i : curIntentions.values()) {
            TrackedIntention ti = this.intentions.get(i.getId());
            if (ti.id < 0) {
                continue;
            }
            var place = placeOf(i, pending, running, selected, containedIn);
            if (place != ti.place) {
                this.recordEntityChange(m, ti.id, "place", place);
                ti.place = place;
            }
            Boolean suspended = i.isSuspended();
            if (!suspended.equals(ti.suspended)) {
                this.recordEntityChange(m, ti.id, "suspended", suspended);
                ti.suspended = suspended;
            }
            Object waiting = containedIn.get(i);
            if (waiting != ti.waitingOn) {
                Long wid = waiting instanceof jason.asSemantics.Event e ? idOf(this.events.get(e)) :
                           waiting instanceof ActionExec a ? this.actions.get(a) : null;
                if (waiting == null || (wid != null && wid >= 0)) {
                    this.recordEntityChange(m, ti.id, "waitingOn", waiting == null ? null : new EntityRef(wid));
                    ti.waitingOn = waiting;
                }
            }
        }
        for (Message msg : mailOrder) {
            if (this.mailbox.containsKey(msg)) {
                continue;
            }
            Long id = this.directory.get(msg.getMsgId(), this.agent);
            if (id == null) {
                Long reply = this.directory.get(msg.getInReplyTo(), msg.getSender());
                id = this.recordEntityBirth(m, null,
                        new EntitySpec.MessageSpec(sendNumber(msg.getMsgId()), SendMode.ASYNCHRONOUS, reply,
                                msg.getSender() == null ? "?" : msg.getSender(), this.agent,
                                new Performative(msg.getIlForce()), String.valueOf(msg.getPropCont())));
                if (id >= 0) {
                    this.directory.put(msg.getMsgId(), this.agent, id);
                }
            }
            this.mailbox.put(msg, id);
            if (id >= 0) {
                this.record(new RecordOccurrenceIn.Arrival(this.agent, m, id));
            }
        }

        for (Iterator<Map.Entry<IntendedMeans, TrackedIM>> it = this.ims.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<IntendedMeans, TrackedIM> e = it.next();
            if (curIMs.contains(e.getKey())) {
                continue;
            }
            TrackedIM value = e.getValue();
            it.remove();
            if (value.id >= 0) {
                this.recordEntityEnd(m, value.id);
            }
        }
        for (Iterator<Map.Entry<ActionExec, Long>> it = this.actions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ActionExec, Long> e = it.next();
            if (curActions.contains(e.getKey())) {
                continue;
            }
            ActionExec a = e.getKey();
            long id = e.getValue();
            it.remove();
            if (id < 0) {
                continue;
            }
            if (step == ReasoningStep.PROCESS_ACTIONS) {
                ActionResult r = a.getResult() ? new ActionResult(Action.Result.SUCCESS, null, null) : new ActionResult(
                        Action.Result.FAILURE, a.getFailureReason() == null ? null : a.getFailureReason().toString(),
                        a.getFailureMsg());
                this.record(new RecordOccurrenceIn.Change(this.agent, m, null, id, "result", r));
            }
            this.recordEntityEnd(m, id);
        }
        for (Iterator<Map.Entry<jason.asSemantics.Event, TrackedEvent>> it = this.events.entrySet().iterator();
             it.hasNext(); ) {
            Map.Entry<jason.asSemantics.Event, TrackedEvent> e = it.next();
            if (curEvents.containsKey(e.getKey())) {
                continue;
            }
            TrackedEvent value = e.getValue();
            it.remove();
            if (value.id >= 0) {
                this.recordEntityEnd(m, value.id);
            }
        }
        for (Iterator<Map.Entry<Integer, TrackedIntention>> it = this.intentions.entrySet().iterator();
             it.hasNext(); ) {
            Map.Entry<Integer, TrackedIntention> e = it.next();
            if (curIntentions.containsKey(e.getKey())) {
                continue;
            }
            TrackedIntention value = e.getValue();
            it.remove();
            if (value.id >= 0) {
                this.recordEntityEnd(m, value.id);
            }
        }
        Set<Literal> curBeliefSet = Collections.newSetFromMap(new IdentityHashMap<>());
        curBeliefSet.addAll(curBeliefs);
        for (Iterator<Map.Entry<Literal, TrackedBelief>> it = this.beliefs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Literal, TrackedBelief> e = it.next();
            if (curBeliefSet.contains(e.getKey())) {
                continue;
            }
            TrackedBelief value = e.getValue();
            it.remove();
            if (value.id >= 0) {
                this.recordEntityEnd(m, value.id);
            }
        }
        Set<Plan> curPlanSet = Collections.newSetFromMap(new IdentityHashMap<>());
        curPlanSet.addAll(curPlans);
        for (Iterator<Map.Entry<Plan, TrackedPlan>> it = this.plans.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Plan, TrackedPlan> e = it.next();
            if (curPlanSet.contains(e.getKey()) || e.getValue().detached) {
                continue;
            }
            Plan key = e.getKey();
            TrackedPlan value = e.getValue();
            it.remove();
            this.planIndexes.remove(key);
            if (value.id >= 0) {
                this.recordEntityEnd(m, value.id);
            }
        }
        for (Iterator<Map.Entry<Message, Long>> it = this.mailbox.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Message, Long> e = it.next();
            if (curMailbox.contains(e.getKey())) {
                continue;
            }
            long id = e.getValue();
            it.remove();
            if (id >= 0) {
                this.record(new RecordOccurrenceIn.End(this.agent, m, null, id));
            }
        }

        for (IntendedMeans im : curIMs) {
            TrackedIM tim = this.ims.get(im);
            if (tim == null || tim.id < 0 || im.getUnif() == null) {
                continue;
            }
            Unifier u = im.getUnif();
            boolean seed = !tim.seeded;
            tim.seeded = true;
            for (VarTerm v : u) {
                String name = v.toString();
                if (name.startsWith("_") && !seed) {
                    continue;
                }
                Term value = u.get(v);
                if (value == null || value.isVar()) {
                    continue;
                }
                String text = value.toString();
                if (!text.equals(tim.bindings.get(name))) {
                    tim.bindings.put(name, text);
                    this.record(new RecordOccurrenceIn.AddBinding(this.agent, m, tim.id, name, text));
                }
            }
        }
    }

    /**
     * Records the birth of new plans and the source changes of tracked plans.
     *
     * @param m       moment of the recorded occurrences
     * @param current plans of the agent in library order
     */
    private void capturePlanChanges(Moment m, List<Plan> current) {
        for (int k = 0; k < current.size(); k++) {
            Plan p = current.get(k);
            TrackedPlan tp = this.plans.get(p);
            List<Annotation> labelAnnots = p.getLabel() == null ? List.of() : JasonTranslator.annotations(p.getLabel());
            Set<Source> sources = new LinkedHashSet<>(JasonTranslator.sources(labelAnnots));
            if (tp == null) {
                long key = orderKey(current, k, this.plans);
                long id = this.recordPlanBirth(m, p, key, labelAnnots, sources);
                this.plans.put(p, new TrackedPlan(id, key, sources));
                continue;
            }
            if (tp.id < 0 || tp.sources.equals(sources)) {
                continue;
            }
            for (Source s : sources) {
                if (!tp.sources.contains(s)) {
                    this.record(
                            new RecordOccurrenceIn.CollectionChange(this.agent, m, this.stepCause, tp.id, "sources", s,
                                    true));
                }
            }
            for (Source s : tp.sources) {
                if (!sources.contains(s)) {
                    this.record(
                            new RecordOccurrenceIn.CollectionChange(this.agent, m, this.stepCause, tp.id, "sources", s,
                                    false));
                }
            }
            tp.sources = sources;
        }
    }

    /**
     * Records the birth of a plan.
     *
     * @param m           moment of the birth
     * @param p           plan that is born
     * @param key         order key of the plan in the library
     * @param labelAnnots annotations of the plan label
     * @param sources     sources declared by the label annotations
     * @return the entity id of the plan, or {@code -1} when the birth was not recorded
     */
    private long recordPlanBirth(Moment m, Plan p, long key, List<Annotation> labelAnnots, Set<Source> sources) {
        List<Annotation> others = new ArrayList<>();
        for (Annotation a : labelAnnots) {
            if (!a.isSource()) {
                others.add(a);
            }
        }
        SourceInfo si = p.getSrcInfo();
        SourceLocation location = null;
        SourceFile f = si == null ? null : this.sourceFiles.get(si.getSrcFile());
        if (f != null) {
            Integer line = si.getSrcLine() <= 0 ? null : si.getBeginSrcLine() > 0 ? si.getBeginSrcLine() : si.getSrcLine();
            location = new SourceLocation(f, line);
            this.registerSource(f);
        }
        String label = p.getLabel() == null ? null : p.getLabel().getFunctor();
        return this.recordEntityBirth(m, this.stepCause,
                new EntitySpec.PlanSpec(label, key, JasonTranslator.trigger(p.getTrigger()),
                        JasonTranslator.context(p.getContext()), this.index(p).deeds(), others, location,
                        new ArrayList<>(sources)));
    }

    /**
     * Resolves the plan entity of an intended means.
     * <p>
     * Falls back to the library plan with the same label and, when none is tracked, records the plan as a detached
     * plan placed after every library plan.
     *
     * @param m moment of a possible birth
     * @param p plan of the intended means
     * @return the entity id of the plan, or {@code -1} when it is not recorded
     */
    private long planIdOf(Moment m, Plan p) {
        TrackedPlan tp = this.plans.get(p);
        if (tp == null && p.getLabel() != null) {
            Plan lib = this.ag.getPL().get(p.getLabel());
            if (lib != null) {
                tp = this.plans.get(lib);
                if (tp != null) {
                    this.planIndexes.putIfAbsent(p, this.index(lib));
                }
            }
        }
        if (tp == null) {
            List<Annotation> labelAnnots = p.getLabel() == null ? List.of() : JasonTranslator.annotations(p.getLabel());
            Set<Source> sources = new LinkedHashSet<>(JasonTranslator.sources(labelAnnots));
            long id = this.recordPlanBirth(m, p, Long.MAX_VALUE / 2, labelAnnots, sources);
            tp = new TrackedPlan(id, Long.MAX_VALUE / 2, sources);
            tp.detached = true;
            this.plans.put(p, tp);
        }
        return tp.id;
    }

    /**
     * Records the birth of new beliefs and the annotation changes of tracked beliefs.
     *
     * @param m       moment of the recorded occurrences
     * @param current beliefs of the agent in belief base order
     */
    private void captureBeliefChanges(Moment m, List<Literal> current) {
        for (int k = 0; k < current.size(); k++) {
            Literal l = current.get(k);
            TrackedBelief tb = this.beliefs.get(l);
            List<Annotation> annots = JasonTranslator.annotations(l);
            if (tb == null) {
                long key = orderKey(current, k, this.beliefs);
                long id = this.recordEntityBirth(m, this.stepCause,
                        new EntitySpec.BeliefSpec(JasonTranslator.withoutAnnotations(l), l.isRule(), key, annots));
                this.beliefs.put(l, new TrackedBelief(id, key, new LinkedHashSet<>(annots)));
                continue;
            }
            if (tb.id < 0) {
                continue;
            }
            Set<Annotation> now = new LinkedHashSet<>(annots);
            if (now.equals(tb.annotations)) {
                continue;
            }
            for (Annotation a : now) {
                if (!tb.annotations.contains(a)) {
                    this.record(
                            new RecordOccurrenceIn.CollectionChange(this.agent, m, this.stepCause, tb.id, "annotations",
                                    a, true));
                }
            }
            for (Annotation a : tb.annotations) {
                if (!now.contains(a)) {
                    this.record(
                            new RecordOccurrenceIn.CollectionChange(this.agent, m, this.stepCause, tb.id, "annotations",
                                    a, false));
                }
            }
            tb.annotations = now;
        }
    }

    /**
     * Records the birth of an entity.
     *
     * @param m     moment of the birth
     * @param cause cause of the birth, or {@code null}
     * @param spec  specification of the entity
     * @return the entity id, or {@code -1} when the birth was not recorded
     */
    private long recordEntityBirth(Moment m, CauseIn cause, EntitySpec spec) {
        RecordOccurrenceOut out = this.record(new RecordOccurrenceIn.Birth(this.agent, m, cause, spec));
        return out == null ? -1 : out.entityId();
    }

    /**
     * Records a timeline change of an entity, omitting the step cause when the entity is the cause itself.
     *
     * @param m        moment of the change
     * @param id       entity id
     * @param timeline name of the changed timeline
     * @param value    new value of the timeline
     */
    private void recordEntityChange(Moment m, long id, String timeline, Object value) {
        this.record(new RecordOccurrenceIn.Change(this.agent, m, id == this.stepCauseId ? null : this.stepCause, id,
                timeline, value));
    }

    /**
     * Records the end of an entity, omitting the step cause when the entity is the cause itself.
     *
     * @param m  moment of the end
     * @param id entity id
     */
    private void recordEntityEnd(Moment m, long id) {
        this.record(new RecordOccurrenceIn.End(this.agent, m, id == this.stepCauseId ? null : this.stepCause, id));
    }

    /**
     * Records an occurrence.
     * <p>
     * When the occurrence is rejected because of its cause, it is recorded again without the cause and
     * {@link #CAUSES_DROPPED} is incremented. Other failures are reported through {@link #reportError(RuntimeException)}.
     *
     * @param in occurrence to record
     * @return the recording result, or {@code null} when the occurrence was not recorded
     */
    private RecordOccurrenceOut record(RecordOccurrenceIn in) {
        try {
            return this.acknowledgeRecording(in, this.ports.recordOccurrence().execute(in));
        } catch (InvariantViolationException e) {
            RecordOccurrenceIn without = withoutCause(in);
            if (without != null && "INV-029".equals(e.getInvariant()) && e.getMessage() != null && e.getMessage()
                    .contains("cause.by")) {
                CAUSES_DROPPED.incrementAndGet();
                try {
                    return this.acknowledgeRecording(without, this.ports.recordOccurrence().execute(without));
                } catch (RuntimeException e2) {
                    this.reportError(e2);
                    return null;
                }
            }
            this.reportError(e);
            return null;
        } catch (RuntimeException e) {
            this.reportError(e);
            return null;
        }
    }

    /**
     * Counts a recorded occurrence that produced a new sequence and marks its phase as active.
     *
     * @param in  recorded occurrence
     * @param out recording result
     * @return the recording result
     */
    private RecordOccurrenceOut acknowledgeRecording(RecordOccurrenceIn in, RecordOccurrenceOut out) {
        if (out.sequence() > 0) {
            this.recorded++;
            this.markActive(in.moment());
        }
        return out;
    }

    /**
     * Runs an action, reporting any runtime exception instead of propagating it.
     *
     * @param r action to run
     * @return {@code true} when the action completed without exception
     */
    private boolean safe(Runnable r) {
        try {
            r.run();
            return true;
        } catch (RuntimeException e) {
            this.reportError(e);
            return false;
        }
    }

    /**
     * Counts a capture failure and logs it.
     * <p>
     * Only the first 20 failures and then every power-of-two failure are logged; the stack trace is included for the
     * first 3.
     *
     * @param e failure to report
     */
    private void reportError(RuntimeException e) {
        long n = CAPTURE_ERRORS.incrementAndGet();
        if (n <= 20 || Long.bitCount(n) == 1) {
            LOGGER.log(Level.WARNING, "Mind: capture failed for " + this.agent + " (" + n + " in total): " + e,
                    n <= 3 ? e : null);
        }
    }

    @Override
    public synchronized void cycleStarting(TransitionSystem ts, int cycleNumber) {
        try {
            this.captureCycleStarting(ts, cycleNumber);
        } catch (RuntimeException e) {
            this.reportError(e);
        }
    }

    @Override
    public synchronized void cycleFinished(TransitionSystem ts, int cycleNumber) {
        try {
            this.captureCycleFinished(ts, cycleNumber);
        } catch (RuntimeException e) {
            this.reportError(e);
        }
    }

    @Override
    public synchronized void phaseStarting(TransitionSystem ts, Phase phase) {
        try {
            this.capturePhaseStarting(ts, phase);
        } catch (RuntimeException e) {
            this.reportError(e);
        }
    }

    @Override
    public synchronized void stepStarting(TransitionSystem ts, Step step) {
        try {
            this.captureStepStarting(ts, step);
        } catch (RuntimeException e) {
            this.reportError(e);
        }
    }

    @Override
    public synchronized void stepFinished(TransitionSystem ts, Step step) {
        try {
            this.captureStepFinished(ts, step);
        } catch (RuntimeException e) {
            this.reportError(e);
        }
    }

    @Override
    public synchronized void planSearchStarting(TransitionSystem ts, jason.asSemantics.Event event,
                                                SearchMethod method) {
        try {
            this.capturePlanSearchStarting(ts, event, method);
        } catch (RuntimeException e) {
            this.reportError(e);
        }
    }

    @Override
    public synchronized void planEvaluated(TransitionSystem ts, Plan plan, boolean relevant, boolean applicable,
                                           List<TermResult> terms) {
        try {
            this.capturePlanEvaluated(ts, plan, relevant, applicable, terms);
        } catch (RuntimeException e) {
            this.reportError(e);
        }
    }

    @Override
    public synchronized void messageSent(TransitionSystem ts, Message m) {
        try {
            this.captureMessageSent(ts, m);
        } catch (RuntimeException e) {
            this.reportError(e);
        }
    }

    @Override
    public String toString() {
        return "JasonCaptureAdapter[" + this.agent + "]";
    }

    /**
     * Element that has an order key.
     * <p>
     * Lets plans and beliefs share the computation of order keys.
     */
    private interface Ordered {

        /**
         * Returns the order key of the element.
         *
         * @return the order key; smaller keys come first
         */
        long orderKey();
    }

    /**
     * Tracked state of a recorded plan.
     * <p>
     * Keeps the entity id, the order key, the current sources and whether the plan is outside the plan library.
     */
    private static final class TrackedPlan implements Ordered {

        /** Entity id of the plan, or {@code -1} when its birth was not recorded. */
        final long id;

        /** Order key of the plan. */
        final long key;

        /** Sources last recorded for the plan. */
        Set<Source> sources;

        /** Whether the plan is not part of the plan library. */
        boolean detached;

        /**
         * Creates the tracked plan with its entity id, order key and sources.
         *
         * @param id      {@link #id}
         * @param key     {@link #key}
         * @param sources {@link #sources}
         */
        TrackedPlan(long id, long key, Set<Source> sources) {
            this.id = id;
            this.key = key;
            this.sources = sources;
        }

        @Override
        public long orderKey() {
            return this.key;
        }
    }

    /**
     * Tracked state of a recorded belief.
     * <p>
     * Keeps the entity id, the order key and the current annotations.
     */
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class TrackedBelief implements Ordered {

        /** Entity id of the belief, or {@code -1} when its birth was not recorded. */
        final long id;

        /** Order key of the belief. */
        final long key;

        /** Annotations last recorded for the belief. */
        Set<Annotation> annotations;

        @Override
        public long orderKey() {
            return this.key;
        }
    }

    /**
     * Tracked state of a recorded event.
     * <p>
     * Keeps the entity id and the last recorded trigger, place and state.
     */
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class TrackedEvent {

        /** Entity id of the event, or {@code -1} when its birth was not recorded. */
        final long id;

        /** Trigger last recorded for the event. */
        Trigger trigger;

        /** Place last recorded for the event. */
        jason.mind.api.core.domain.model.entities.Event.Place place;

        /** State last recorded for the event. */
        jason.mind.api.core.domain.model.entities.Event.State state;
    }

    /**
     * Tracked state of a recorded intention.
     * <p>
     * Keeps the entity id and the last recorded place, suspension and holding event or action.
     */
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class TrackedIntention {

        /** Entity id of the intention, or {@code -1} when its birth was not recorded. */
        final long id;

        /** Place last recorded for the intention. */
        jason.mind.api.core.domain.model.entities.Intention.Place place;

        /** Suspension last recorded for the intention. */
        Boolean suspended;

        /** Event or action the intention was last recorded waiting on, or {@code null}. */
        Object waitingOn;
    }

    /**
     * Tracked state of a recorded intended means.
     * <p>
     * Keeps the entity id and the variable bindings already recorded.
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class TrackedIM {

        /** Entity id of the intended means, or {@code -1} when its birth was not recorded. */
        final long id;

        /** Text of each variable binding already recorded, indexed by variable name. */
        final Map<String, String> bindings = new HashMap<>();

        /** Whether the first pass over the unifier, which also records the renamed variables, already ran. */
        boolean seeded;
    }
}
