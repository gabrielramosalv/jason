package jason.mind.api.core.application.settings;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility that finds, for each agent, the entities still alive that started before the retention horizon.
 * The horizon is the first cycle kept by the retention, counted back from the last closed cycle.
 *
 * @author Gabriel Lima
 */
final class LiveBeyondHorizon {

    /** Entity type names that are checked against the horizon. */
    private static final Set<String> TYPES = Set.of("belief", "intention", "event", "message", "action",
            "intendedMeans", "timer");

    /**
     * Prevents instantiation of this utility class.
     */
    private LiveBeyondHorizon() {
    }

    /**
     * Counts, per agent, the entities it started that are alive at its last closed cycle and started before
     * the horizon, together with the oldest of them.
     *
     * @param query     read-only access to the history
     * @param retention number of cycles retained
     * @return one entry per agent that has such entities
     */
    static List<SettingsOut.LiveBeyondHorizon> compute(HistoryQueryRepositoryPort query, int retention) {
        List<SettingsOut.LiveBeyondHorizon> r = new ArrayList<>();
        for (Agent a : query.agents()) {
            CycleRecord last = query.lastClosedCycle(a.getName()).orElse(null);
            if (last == null) {
                continue;
            }
            int horizon = last.cycle() - retention + 1;
            if (horizon <= 0) {
                continue;
            }
            int count = 0;
            Entity oldest = null;
            for (Entity e : query.entitiesOfTypes(a.getName(), TYPES)) {
                if (e.getStart() == null || !e.getStart().agent().equals(a.getName())) {
                    continue;
                }
                if (!HistoryView.aliveAt(e, last.sequenceEnd())) {
                    continue;
                }
                if (e.getStart().cycle() >= horizon) {
                    continue;
                }
                count++;
                if (oldest == null || e.getStart().sequence() < oldest.getStart().sequence()) {
                    oldest = e;
                }
            }
            if (count > 0) {
                r.add(new SettingsOut.LiveBeyondHorizon(a.getName(), count,
                        new SettingsOut.Oldest(oldest.typeName(), EntityIds.format(oldest), oldest.label(),
                                oldest.getStart().cycle())));
            }
        }
        return r;
    }
}
