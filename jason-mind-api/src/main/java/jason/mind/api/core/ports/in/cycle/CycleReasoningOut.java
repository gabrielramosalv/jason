package jason.mind.api.core.ports.in.cycle;

import jason.mind.api.core.ports.in.history.EntityRefOut;

import java.util.List;

/**
 * Output with the reasoning of an agent in a cycle, step by step.
 *
 * @param agent name of the agent
 * @param cycle number of the cycle
 * @param steps reasoning steps of the cycle
 * @author Gabriel Lima
 */
public record CycleReasoningOut(String agent, int cycle, List<StepOut> steps) {

    /**
     * Reasoning step.
     *
     * @param step         name of the step
     * @param phase        name of the phase
     * @param iteration    iteration inside the cycle
     * @param ordinal      position inside the step
     * @param searchMethod plan search method, or null
     * @param inputs       entities given to the step
     * @param chosen       chosen entity, or null
     * @param candidates   candidates considered
     * @param created      entities created in the step
     * @param changed      changes made in the step
     * @param removed      entities removed in the step
     */
    public record StepOut(String step, String phase, int iteration, int ordinal, String searchMethod,
                          List<EntityRefOut> inputs, EntityRefOut chosen, List<CandidateOut> candidates,
                          List<EntityRefOut> created, List<ChangeOut> changed, List<EntityRefOut> removed) {

    }

    /**
     * Candidate of a step.
     *
     * @param type   type of the entity
     * @param id     identifier of the entity
     * @param label  label of the entity
     * @param rule   rule that explains the outcome, or null
     * @param chosen whether the candidate was chosen
     * @param terms  context term evaluations of a candidate plan
     */
    public record CandidateOut(String type, String id, String label, String rule, boolean chosen, List<TermOut> terms) {

    }

    /**
     * Evaluation of a context term.
     *
     * @param index    position of the term
     * @param verdict  verdict of the term
     * @param bindings bindings produced
     */
    public record TermOut(int index, String verdict, List<BindingOut> bindings) {

    }

    /**
     * Variable binding.
     *
     * @param name  name of the variable
     * @param value bound value
     */
    public record BindingOut(String name, String value) {

    }

    /**
     * Change of an entity timeline.
     *
     * @param type     type of the entity
     * @param id       identifier of the entity
     * @param label    label of the entity
     * @param timeline name of the timeline
     * @param value    new value
     */
    public record ChangeOut(String type, String id, String label, String timeline, String value) {

    }
}
