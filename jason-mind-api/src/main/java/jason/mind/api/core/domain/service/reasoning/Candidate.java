package jason.mind.api.core.domain.service.reasoning;

import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.trace.PlanEvaluation;

/**
 * Candidate considered by a selection step.
 * Tells whether it was chosen and which rule explains its outcome.
 *
 * @param entity     candidate entity
 * @param rule       rule that explains the outcome, or null
 * @param chosen     whether the candidate was chosen
 * @param evaluation context evaluation of a candidate plan, or null
 * @author Gabriel Lima
 */
public record Candidate(Entity entity, SelectionRule rule, boolean chosen, PlanEvaluation evaluation) {

}
