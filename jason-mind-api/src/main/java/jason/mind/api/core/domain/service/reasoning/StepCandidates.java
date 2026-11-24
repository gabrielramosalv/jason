package jason.mind.api.core.domain.service.reasoning;

import jason.mind.api.core.domain.model.timeline.Entity;

import java.util.List;

/**
 * Candidates of a selection step.
 * Holds the inputs of the step, the chosen entity and every candidate considered.
 *
 * @param inputs     entities given to the step
 * @param chosen     chosen entity, or null
 * @param candidates candidates considered
 * @author Gabriel Lima
 */
public record StepCandidates(List<Entity> inputs, Entity chosen, List<Candidate> candidates) {

    /**
     * Returns step candidates without inputs, choice or candidates.
     *
     * @return empty step candidates
     */
    public static StepCandidates empty() {
        return new StepCandidates(List.of(), null, List.of());
    }
}
