package jason.mind.api.core.domain.service.history;

import jason.mind.api.core.domain.model.history.Effect;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain service that derives the effects an entity caused on other entities.
 *
 * @author Gabriel Lima
 */
public final class EffectsDomainService {

    /**
     * Converts the records caused by an entity into effects, skipping records of the entity itself.
     *
     * @param entity entity whose effects are derived
     * @param caused occurrence records to inspect
     * @return effects on other entities whose cause is the given entity, in record order
     */
    public List<Effect> effectsOf(Entity entity, List<OccurrenceRecord> caused) {
        List<Effect> r = new ArrayList<>();
        for (OccurrenceRecord rec : caused) {
            if (rec.entity() == entity) {
                continue;
            }
            if (rec.occurrence().cause() == null || rec.occurrence().cause().by() != entity) {
                continue;
            }
            Effect.Kind kind = switch (rec.kind()) {
                case START -> Effect.Kind.START;
                case END -> Effect.Kind.END;
                default -> Effect.Kind.CHANGE;
            };
            r.add(new Effect(kind, rec.entity(), rec.occurrence().moment(), rec.sequence(), rec.timeline(),
                    rec.value()));
        }
        return r;
    }
}
