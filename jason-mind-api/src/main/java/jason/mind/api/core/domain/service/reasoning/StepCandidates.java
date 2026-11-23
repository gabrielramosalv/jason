package jason.mind.api.core.domain.service.reasoning;

import java.util.List;

import jason.mind.api.core.domain.model.timeline.Entity;

public record StepCandidates(List<Entity<?>> inputs, Entity<?> chosen, List<Candidate> candidates) {

    public static StepCandidates empty() {
        return new StepCandidates(List.of(), null, List.of());
    }
}
