package jason.mind.api.core.application.lifecycle;

import jason.mind.api.core.domain.model.entities.Action;
import jason.mind.api.core.domain.model.entities.Belief;
import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.Effect;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;

import java.util.List;

/**
 * Utility that assigns lifecycle codes to occurrences and effects.
 * A code names what an occurrence means for its entity, such as a belief perceived or an event handled,
 * based on the entity type, the occurrence kind, its cause and the reasoning step where it happened.
 *
 * @author Gabriel Lima
 */
final class LifecycleCodes {

    /**
     * Prevents instantiation of this utility class.
     */
    private LifecycleCodes() {
    }

    /**
     * Assigns the code of an occurrence of the entity itself.
     *
     * @param e       entity that owns the occurrence
     * @param r       occurrence record being classified
     * @param effects effects caused by the entity, used to classify the end of an event
     * @return the lifecycle code, or {@code null} when the occurrence has no code
     */
    /** Quem executa uma deed: o objetivo do topo da pilha ou, sem ele rastreado, a intenção. */
    private static boolean isDoer(Entity by) {
        return by instanceof IntendedMeans || by instanceof Intention;
    }

    static String describeOwn(Entity e, OccurrenceRecord r, List<Effect> effects) {
        Entity by = r.occurrence().cause() == null ? null : r.occurrence().cause().by();
        ReasoningStep step = r.occurrence().moment().step();
        if (e instanceof Belief) {
            if (r.kind() == OccurrenceRecord.Kind.START) {
                if (by == null && step == ReasoningStep.PERCEIVE) {
                    return "BELIEF_PERCEIVED";
                }
                if (by instanceof Message) {
                    return "BELIEF_ADDED_BY_MESSAGE";
                }
                if (isDoer(by)) {
                    return "BELIEF_ADDED_BY_INTENTION";
                }
                return null;
            }
            if (r.kind() == OccurrenceRecord.Kind.END) {
                if (by == null && step == ReasoningStep.PERCEIVE) {
                    return "BELIEF_REMOVED_BY_ENVIRONMENT";
                }
                if (isDoer(by)) {
                    return "BELIEF_REMOVED_BY_INTENTION";
                }
                return null;
            }
            return null;
        }
        if (e instanceof Message) {
            if (r.kind() == OccurrenceRecord.Kind.ARRIVAL) {
                return "MESSAGE_RECEIVED";
            }
            if (r.kind() == OccurrenceRecord.Kind.END && step == ReasoningStep.PROCESS_MESSAGES) {
                return "MESSAGE_READ";
            }
            return null;
        }
        if (e instanceof Event ev) {
            return describeEvent(ev, r, by, step, effects);
        }
        if (e instanceof Intention) {
            switch (r.kind()) {
                case START -> {
                    return "INTENTION_CREATED";
                }
                case END -> {
                    return "INTENTION_CLEARED";
                }
                case CHANGE -> {
                    Object v = r.value();
                    if ("place".equals(r.timeline())) {
                        if (v == Intention.Place.SELECTED_INTENTION) {
                            return "INTENTION_SELECTED";
                        }
                        if (v == Intention.Place.PENDING_INTENTIONS) {
                            return "INTENTION_WAITING_EVENT";
                        }
                        if (v == Intention.Place.RUNNING) {
                            return "INTENTION_BACK_IN_QUEUE";
                        }
                    }
                    if ("waitingOn".equals(r.timeline()) && v instanceof Action) {
                        return "INTENTION_WAITING_ACTION";
                    }
                    if ("suspended".equals(r.timeline()) && Boolean.TRUE.equals(v)) {
                        return "INTENTION_SUSPENDED";
                    }
                    return null;
                }
                default -> {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Assigns the code of an effect caused by an entity on another entity.
     *
     * @param x       entity that caused the effect
     * @param f       effect being classified
     * @param subgoal whether the affected entity is an intended means pushed as a subgoal
     * @return the lifecycle code, or {@code null} when the effect has no code
     */
    static String describeEffect(Entity x, Effect f, boolean subgoal) {
        if (x instanceof Message) {
            if (f.kind() == Effect.Kind.START && f.entity() instanceof Belief) {
                return "MESSAGE_GENERATED_BELIEF";
            }
            if (f.kind() == Effect.Kind.START && f.entity() instanceof Event) {
                return "MESSAGE_GENERATED_EVENT";
            }
            if (f.kind() == Effect.Kind.CHANGE && f.entity() instanceof Belief) {
                return "MESSAGE_MODIFIED_BELIEF";
            }
            return null;
        }
        if (x instanceof Event) {
            if (f.kind() != Effect.Kind.START) {
                return null;
            }
            if (f.entity() instanceof Intention) {
                return "EVENT_GENERATED_INTENTION";
            }
            if (f.entity() instanceof IntendedMeans && subgoal) {
                return "EVENT_GENERATED_SUBGOAL";
            }
            if (f.entity() instanceof Event other) {
                Trigger t = other.getBirthTrigger();
                return t.operator() == Trigger.Operator.DELETE && t.type() == Trigger.Type.ACHIEVE ?
                       "EVENT_GENERATED_FAILURE_EVENT" : "EVENT_REQUEUED";
            }
        }
        return null;
    }

    /**
     * Assigns the code of an occurrence of an event.
     *
     * @param ev      event that owns the occurrence
     * @param r       occurrence record being classified
     * @param by      entity that caused the occurrence, or {@code null}
     * @param step    reasoning step where the occurrence happened
     * @param effects effects caused by the event
     * @return the lifecycle code, or {@code null} when the occurrence has no code
     */
    private static String describeEvent(Event ev, OccurrenceRecord r, Entity by, ReasoningStep step, List<Effect> effects) {
        switch (r.kind()) {
            case START -> {
                if (by == null && step == ReasoningStep.PERCEIVE) {
                    return "EVENT_TRIGGERED_BY_PERCEPTION";
                }
                if (by instanceof Message) {
                    return "EVENT_TRIGGERED_BY_MESSAGE";
                }
                if (isDoer(by)) {
                    return "EVENT_TRIGGERED_BY_INTENTION";
                }
                if (by instanceof Event) {
                    return "EVENT_TRIGGERED_BY_FAILURE";
                }
                if (by instanceof Action) {
                    return "EVENT_TRIGGERED_BY_ACTION_FAILURE";
                }
                return null;
            }
            case CHANGE -> {
                if ("place".equals(r.timeline()) && r.value() == Event.Place.SELECTED) {
                    return "EVENT_SELECTED";
                }
                if ("state".equals(r.timeline()) && r.value() == Event.State.NO_APPLICABLE) {
                    return "EVENT_NO_APPLICABLE_PLAN";
                }
                return null;
            }
            case END -> {
                boolean hasEffect = false;
                boolean intentionContinued = false;
                for (Effect f : effects) {
                    if (f.sequence() < r.sequence() || !f.moment().equals(r.occurrence().moment())) {
                        continue;
                    }
                    if (f.kind() == Effect.Kind.START) {
                        hasEffect = true;
                    }
                    if (f.kind() == Effect.Kind.CHANGE && f.entity() instanceof Intention) {
                        intentionContinued = true;
                    }
                }
                if (step == ReasoningStep.ADD_INTENDED_MEANS) {
                    return "EVENT_HANDLED";
                }
                if (step == ReasoningStep.FIND_OPTION) {
                    if (hasEffect) {
                        return "EVENT_HANDLED";
                    }
                    if (intentionContinued) {
                        return "EVENT_INTENTION_CONTINUED";
                    }
                    Trigger t = ev.getTrigger().before(r.sequence());
                    if (t != null && t.type() == Trigger.Type.ACHIEVE && t.operator() == Trigger.Operator.ADD) {
                        return "EVENT_GOAL_FAILED";
                    }
                    return "EVENT_DISCARDED";
                }
                return "EVENT_DISCARDED";
            }
            default -> {
                return null;
            }
        }
    }
}
