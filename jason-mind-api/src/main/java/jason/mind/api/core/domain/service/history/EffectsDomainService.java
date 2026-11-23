package jason.mind.api.core.domain.service.history;

import java.util.ArrayList;
import java.util.List;

import jason.mind.api.core.domain.model.history.Effect;
import jason.mind.api.core.domain.model.history.OccurrenceRecord;
import jason.mind.api.core.domain.model.timeline.Entity;

public final class EffectsDomainService {

    public List<Effect> effectsOf(Entity<?> entity, List<OccurrenceRecord> caused) {
        List<Effect> r = new ArrayList<>();
        for (OccurrenceRecord rec : caused) {
            if (rec.entity() == entity) continue;
            if (rec.occurrence().cause() == null || rec.occurrence().cause().by() != entity) continue;
            Effect.Kind kind = switch (rec.kind()) {
                case START -> Effect.Kind.START;
                case END -> Effect.Kind.END;
                default -> Effect.Kind.CHANGE;
            };
            r.add(new Effect(kind, rec.entity(), rec.occurrence().moment(), rec.sequence(), rec.timeline(), rec.value()));
        }
        return r;
    }
}
