package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.timeline.Cause;
import jason.mind.api.core.domain.model.timeline.CodeLocation;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.ports.in.capture.CauseIn;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;

final class CauseResolver {

    private final HistoryRepositoryPort repository;

    CauseResolver(HistoryRepositoryPort repository) {
        this.repository = repository;
    }

    Cause resolve(String agent, CauseIn in) {
        if (in == null) return null;
        Entity<?> by = entity(agent, in.byId());
        if (by instanceof Intention intention && in.planId() != null) {
            Plan plan = (Plan) entity(agent, in.planId());
            return Cause.byIntention(intention, new CodeLocation(plan, in.deedIndex()));
        }
        return Cause.by(by);
    }

    Entity<?> entity(String agent, long id) {
        return repository.findEntity(agent, id).orElseThrow(() -> new UnknownEntityException(agent, id));
    }
}
