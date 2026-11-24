package jason.mind.api.core.application.capture;

import jason.mind.api.core.domain.model.entities.Intention;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.timeline.Cause;
import jason.mind.api.core.domain.model.timeline.CodeLocation;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.ports.in.capture.CauseIn;
import jason.mind.api.core.ports.out.history.HistoryRepositoryPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Resolver that turns entity ids received by the capture use cases into recorded entities and causes.
 * It looks entities up in the history of an agent and fails when an id is unknown.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class CauseResolver {

    /** History where the referenced entities are looked up. */
    private final HistoryRepositoryPort repository;

    /**
     * Resolves the cause of an occurrence. An intention with a plan id yields a cause located at a
     * deed of that plan; any other entity yields a plain cause.
     *
     * @param agent name of the agent whose history holds the entities
     * @param in    cause description, possibly {@code null}
     * @return the resolved cause, or {@code null} when no cause was given
     * @throws UnknownEntityException if a referenced entity does not exist in the agent's history
     */
    Cause resolve(String agent, CauseIn in) {
        if (in == null) {
            return null;
        }
        Entity by = this.entity(agent, in.byId());
        if (by instanceof Intention intention && in.planId() != null) {
            Plan plan = (Plan) this.entity(agent, in.planId());
            return Cause.byIntention(intention, new CodeLocation(plan, in.deedIndex()));
        }
        return Cause.by(by);
    }

    /**
     * Finds an entity in the history of an agent.
     *
     * @param agent name of the agent whose history holds the entity
     * @param id    id of the entity
     * @return the recorded entity
     * @throws UnknownEntityException if the entity does not exist in the agent's history
     */
    Entity entity(String agent, long id) {
        return this.repository.findEntity(agent, id).orElseThrow(() -> new UnknownEntityException(agent, id));
    }
}
