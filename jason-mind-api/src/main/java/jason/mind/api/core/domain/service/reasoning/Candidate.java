package jason.mind.api.core.domain.service.reasoning;

import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.trace.PlanEvaluation;

public record Candidate(Entity<?> entity, String rule, boolean chosen, PlanEvaluation evaluation) {
}
