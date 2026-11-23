package jason.mind.api.core.application.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

final class LiveBeyondHorizon {

    private static final Set<String> TYPES = Set.of("belief", "intention", "event", "message", "action", "intendedMeans", "timer");

    private LiveBeyondHorizon() {
    }

    static List<SettingsOut.LiveBeyondHorizon> compute(HistoryQueryRepositoryPort query, int retention) {
        List<SettingsOut.LiveBeyondHorizon> r = new ArrayList<>();
        for (Agent a : query.agents()) {
            CycleRecord last = query.lastClosedCycle(a.getName()).orElse(null);
            if (last == null) continue;
            int horizon = last.cycle() - retention + 1;
            if (horizon <= 0) continue;
            int count = 0;
            Entity<?> oldest = null;
            for (Entity<?> e : query.entitiesOfTypes(a.getName(), TYPES)) {
                if (e.getStart() == null || !e.getStart().agent().equals(a.getName())) continue;
                if (!HistoryView.aliveAt(e, last.sequenceEnd())) continue;
                if (e.getStart().cycle() >= horizon) continue;
                count++;
                if (oldest == null || e.getStart().sequence() < oldest.getStart().sequence()) oldest = e;
            }
            if (count > 0)
                r.add(new SettingsOut.LiveBeyondHorizon(a.getName(), count, new SettingsOut.Oldest(oldest.typeName(),
                        EntityIds.format(oldest), oldest.label(), oldest.getStart().cycle())));
        }
        return r;
    }
}
