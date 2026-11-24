package jason.mind.api.core.domain.service.reasoning;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.trace.PlanEvaluation;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.domain.model.trace.Verdict;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service that lists the candidates of the event, option and intention selection steps
 * and tags each one with the rule that explains its outcome.
 *
 * @author Gabriel Lima
 */
public final class CandidatesDomainService {

    /**
     * Tells whether a plan trigger may match an event trigger. A variable functor matches any literal;
     * otherwise functor and arity must be equal.
     *
     * @param planTrigger  trigger of the plan
     * @param eventTrigger trigger of the event
     * @return true if operator and type are equal and the literal may match
     */
    public static boolean isCandidate(Trigger planTrigger, Trigger eventTrigger) {
        if (planTrigger.operator() != eventTrigger.operator() || planTrigger.type() != eventTrigger.type()) {
            return false;
        }
        String f = planTrigger.functor();
        if (!f.isEmpty() && (Character.isUpperCase(f.charAt(0)) || f.charAt(0) == '_')) {
            return true;
        }
        return f.equals(eventTrigger.functor()) && planTrigger.arity() == eventTrigger.arity();
    }

    /**
     * Tells whether an applicable plan appears before an index.
     *
     * @param plans       relevant plans in order
     * @param evaluations evaluations by plan
     * @param index       index before which the plans are inspected
     * @return true if some earlier plan was evaluated as applicable
     */
    private static boolean firstApplicableBefore(List<Plan> plans, Map<Plan, PlanEvaluation> evaluations, int index) {
        for (int j = 0; j < index; j++) {
            PlanEvaluation pe = evaluations.get(plans.get(j));
            if (pe != null && pe.isApplicable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a context evaluation has a false term.
     *
     * @param pe evaluation to inspect
     * @return true if any term was false
     */
    private static boolean hasFalse(PlanEvaluation pe) {
        return pe.context().stream().anyMatch(t -> t.result() == Verdict.FALSE);
    }

    /**
     * Lists the candidates of the event selection.
     *
     * @param queueBefore event queue before the selection
     * @param chosen      selected event, or null
     * @param config      configuration of the agent
     * @return queue as inputs, the chosen event and a candidate per event, including a chosen event outside the queue
     */
    public StepCandidates selectEvent(List<Event> queueBefore, Event chosen, AgentConfig config) {
        List<Candidate> candidates = new ArrayList<>();
        for (Event e : queueBefore) {
            SelectionRule rule = null;
            if (e == chosen) {
                if (e.isAtomic()) {
                    rule = SelectionRule.SELECT_EVENT_ATOMIC_PRIORITY;
                } else if (!config.selectEventDefault()) {
                    rule = SelectionRule.SELECT_EVENT_CUSTOM;
                } else if (queueBefore.get(0) == e) {
                    rule = SelectionRule.SELECT_EVENT_FIFO;
                }
            }
            candidates.add(new Candidate(e, rule, e == chosen, null));
        }
        if (chosen != null && !queueBefore.contains(chosen)) {
            candidates.add(new Candidate(chosen, null, true, null));
        }
        return new StepCandidates(new ArrayList<>(queueBefore), chosen, candidates);
    }

    /**
     * Lists the candidates of the intention selection.
     *
     * @param queueBefore intention queue before the selection
     * @param chosen      selected intention, or null
     * @param config      configuration of the agent
     * @return queue as inputs, the chosen intention and a candidate per intention, including a chosen intention
     * outside the queue
     */
    public StepCandidates selectIntention(List<Intention> queueBefore, Intention chosen, AgentConfig config) {
        List<Candidate> candidates = new ArrayList<>();
        for (Intention i : queueBefore) {
            SelectionRule rule = null;
            if (i == chosen && config.selectIntentionDefault() && queueBefore.get(0) == i) {
                rule = SelectionRule.SELECT_INTENTION_ROUND_ROBIN;
            }
            candidates.add(new Candidate(i, rule, i == chosen, null));
        }
        if (chosen != null && !queueBefore.contains(chosen)) {
            candidates.add(new Candidate(chosen, null, true, null));
        }
        return new StepCandidates(new ArrayList<>(queueBefore), chosen, candidates);
    }

    /**
     * Lists the candidates of the option search for an event.
     *
     * @param event        event being handled
     * @param eventTrigger trigger of the event at the time of the search
     * @param plansByOrder plans of the library in order
     * @param selection    traced plan selection, or null when not traced
     * @param chosen       chosen plan, or null
     * @param config       configuration of the agent
     * @return event as input, the chosen plan and a candidate per relevant plan
     */
    public StepCandidates findOption(Event event, Trigger eventTrigger, List<Plan> plansByOrder,
                                     PlanSelection selection, Plan chosen, AgentConfig config) {
        List<Plan> relevantByIndex = new ArrayList<>();
        for (Plan p : plansByOrder) {
            if (isCandidate(p.getTrigger(), eventTrigger)) {
                relevantByIndex.add(p);
            }
        }
        if (chosen != null && !relevantByIndex.contains(chosen)) {
            relevantByIndex.add(chosen);
        }
        Map<Plan, PlanEvaluation> evaluations = new IdentityHashMap<>();
        if (selection != null) {
            for (PlanEvaluation pe : selection.evaluations()) {
                evaluations.put(pe.plan(), pe);
            }
        }
        PlanSelection.SearchMethod method = selection == null ? PlanSelection.SearchMethod.FIRST_FOUND :
                                            selection.searchMethod();

        List<Candidate> candidates = new ArrayList<>();
        int chosenIndex = chosen == null ? -1 : relevantByIndex.indexOf(chosen);
        for (int i = 0; i < relevantByIndex.size(); i++) {
            Plan p = relevantByIndex.get(i);
            PlanEvaluation pe = evaluations.get(p);
            SelectionRule rule;
            if (method == PlanSelection.SearchMethod.FIRST_FOUND) {
                if (p == chosen) {
                    rule = firstApplicableBefore(relevantByIndex, evaluations, i) ? null : SelectionRule.PLAN_FIRST_FOUND;
                } else if (chosenIndex >= 0 && i > chosenIndex) {
                    rule = SelectionRule.PLAN_NOT_EVALUATED;
                } else {
                    rule = pe != null && hasFalse(pe) ? SelectionRule.PLAN_NOT_APPLICABLE : null;
                }
            } else {
                if (p == chosen) {
                    if (!config.selectOptionDefault()) {
                        rule = SelectionRule.PLAN_CUSTOM;
                    } else {
                        rule = firstApplicableBefore(relevantByIndex, evaluations, i) ? null : SelectionRule.PLAN_FIRST_FOUND;
                    }
                } else if (pe == null) {
                    rule = SelectionRule.PLAN_NOT_RELEVANT;
                } else if (!pe.isApplicable()) {
                    rule = hasFalse(pe) ? SelectionRule.PLAN_NOT_APPLICABLE : null;
                } else {
                    rule = SelectionRule.PLAN_NOT_SELECTED;
                }
            }
            candidates.add(new Candidate(p, rule, p == chosen, pe));
        }
        List<Entity> inputs = new ArrayList<>();
        inputs.add(event);
        return new StepCandidates(inputs, chosen, candidates);
    }
}
