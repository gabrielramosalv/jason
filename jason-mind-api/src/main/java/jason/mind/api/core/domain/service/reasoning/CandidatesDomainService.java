package jason.mind.api.core.domain.service.reasoning;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.trace.PlanEvaluation;
import jason.mind.api.core.domain.model.trace.PlanSelection;
import jason.mind.api.core.domain.model.trace.Verdict;

public final class CandidatesDomainService {

    public static final String SELECT_EVENT_FIFO = "SELECT_EVENT_FIFO";
    public static final String SELECT_EVENT_ATOMIC_PRIORITY = "SELECT_EVENT_ATOMIC_PRIORITY";
    public static final String SELECT_EVENT_CUSTOM = "SELECT_EVENT_CUSTOM";
    public static final String PLAN_NOT_APPLICABLE = "PLAN_NOT_APPLICABLE";
    public static final String PLAN_FIRST_FOUND = "PLAN_FIRST_FOUND";
    public static final String PLAN_NOT_EVALUATED = "PLAN_NOT_EVALUATED";
    public static final String PLAN_NOT_RELEVANT = "PLAN_NOT_RELEVANT";
    public static final String PLAN_NOT_SELECTED = "PLAN_NOT_SELECTED";
    public static final String PLAN_CUSTOM = "PLAN_CUSTOM";
    public static final String SELECT_INTENTION_ROUND_ROBIN = "SELECT_INTENTION_ROUND_ROBIN";

    public StepCandidates selectEvent(List<Event> queueBefore, Event chosen, AgentConfig config) {
        List<Candidate> candidates = new ArrayList<>();
        for (Event e : queueBefore) {
            String rule = null;
            if (e == chosen) {
                if (e.isAtomic()) rule = SELECT_EVENT_ATOMIC_PRIORITY;
                else if (!config.selectEventDefault()) rule = SELECT_EVENT_CUSTOM;
                else if (queueBefore.get(0) == e) rule = SELECT_EVENT_FIFO;
            }
            candidates.add(new Candidate(e, rule, e == chosen, null));
        }
        if (chosen != null && !queueBefore.contains(chosen))
            candidates.add(new Candidate(chosen, null, true, null));
        return new StepCandidates(new ArrayList<>(queueBefore), chosen, candidates);
    }

    public StepCandidates selectIntention(List<Intention> queueBefore, Intention chosen, AgentConfig config) {
        List<Candidate> candidates = new ArrayList<>();
        for (Intention i : queueBefore) {
            String rule = null;
            if (i == chosen && config.selectIntentionDefault() && queueBefore.get(0) == i)
                rule = SELECT_INTENTION_ROUND_ROBIN;
            candidates.add(new Candidate(i, rule, i == chosen, null));
        }
        if (chosen != null && !queueBefore.contains(chosen))
            candidates.add(new Candidate(chosen, null, true, null));
        return new StepCandidates(new ArrayList<>(queueBefore), chosen, candidates);
    }

    public StepCandidates findOption(Event event, Trigger eventTrigger, List<Plan> plansByOrder, PlanSelection selection,
                                     Plan chosen, AgentConfig config) {
        List<Plan> relevantByIndex = new ArrayList<>();
        for (Plan p : plansByOrder)
            if (isCandidate(p.getTrigger(), eventTrigger)) relevantByIndex.add(p);
        if (chosen != null && !relevantByIndex.contains(chosen)) relevantByIndex.add(chosen);
        Map<Plan, PlanEvaluation> evaluations = new IdentityHashMap<>();
        if (selection != null)
            for (PlanEvaluation pe : selection.evaluations()) evaluations.put(pe.plan(), pe);
        PlanSelection.SearchMethod method = selection == null ? PlanSelection.SearchMethod.FIRST_FOUND : selection.searchMethod();

        List<Candidate> candidates = new ArrayList<>();
        int chosenIndex = chosen == null ? -1 : relevantByIndex.indexOf(chosen);
        for (int i = 0; i < relevantByIndex.size(); i++) {
            Plan p = relevantByIndex.get(i);
            PlanEvaluation pe = evaluations.get(p);
            String rule;
            if (method == PlanSelection.SearchMethod.FIRST_FOUND) {
                if (p == chosen) rule = firstApplicableBefore(relevantByIndex, evaluations, i) ? null : PLAN_FIRST_FOUND;
                else if (chosenIndex >= 0 && i > chosenIndex) rule = PLAN_NOT_EVALUATED;
                else rule = pe != null && hasFalse(pe) ? PLAN_NOT_APPLICABLE : null;
            } else {
                if (p == chosen) {
                    if (!config.selectOptionDefault()) rule = PLAN_CUSTOM;
                    else rule = firstApplicableBefore(relevantByIndex, evaluations, i) ? null : PLAN_FIRST_FOUND;
                } else if (pe == null) rule = PLAN_NOT_RELEVANT;
                else if (!pe.isApplicable()) rule = hasFalse(pe) ? PLAN_NOT_APPLICABLE : null;
                else rule = PLAN_NOT_SELECTED;
            }
            candidates.add(new Candidate(p, rule, p == chosen, pe));
        }
        List<Entity<?>> inputs = new ArrayList<>();
        inputs.add(event);
        return new StepCandidates(inputs, chosen, candidates);
    }

    private static boolean firstApplicableBefore(List<Plan> plans, Map<Plan, PlanEvaluation> evaluations, int index) {
        for (int j = 0; j < index; j++) {
            PlanEvaluation pe = evaluations.get(plans.get(j));
            if (pe != null && pe.isApplicable()) return true;
        }
        return false;
    }

    private static boolean hasFalse(PlanEvaluation pe) {
        return pe.context().stream().anyMatch(t -> t.result() == Verdict.FALSE);
    }

    public static boolean isCandidate(Trigger planTrigger, Trigger eventTrigger) {
        if (planTrigger.operator() != eventTrigger.operator() || planTrigger.type() != eventTrigger.type()) return false;
        String f = planTrigger.functor();
        if (!f.isEmpty() && (Character.isUpperCase(f.charAt(0)) || f.charAt(0) == '_')) return true;
        return f.equals(eventTrigger.functor()) && planTrigger.arity() == eventTrigger.arity();
    }
}
