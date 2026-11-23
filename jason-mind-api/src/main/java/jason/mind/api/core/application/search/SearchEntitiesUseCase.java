package jason.mind.api.core.application.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.domain.model.entities.Action;
import jason.mind.api.core.domain.model.entities.Belief;
import jason.mind.api.core.domain.model.entities.Event;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.TimelineEntry;
import jason.mind.api.core.ports.in.search.SearchEntitiesIn;
import jason.mind.api.core.ports.in.search.SearchEntitiesPort;
import jason.mind.api.core.ports.in.search.SearchOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

public class SearchEntitiesUseCase implements SearchEntitiesPort {

    public static final Set<String> TYPES = Set.of("belief", "intention", "event", "message", "action");
    public static final int DEFAULT_LIMIT = 200;

    private final HistoryQueryRepositoryPort historyQuery;

    public SearchEntitiesUseCase(HistoryQueryRepositoryPort historyQuery) {
        this.historyQuery = historyQuery;
    }

    @Override
    public SearchOut execute(SearchEntitiesIn in) {
        HistoryView.agent(historyQuery, in.agent());
        if (in.q() == null || in.q().isBlank()) throw new InvalidQueryException("q must not be empty");
        Set<String> types = in.types() == null || in.types().isEmpty() ? TYPES : in.types();
        for (String t : types) if (!TYPES.contains(t)) throw new InvalidQueryException("unknown type: " + t);
        int limit = in.limit() == null ? DEFAULT_LIMIT : in.limit();
        if (limit < 1) throw new InvalidQueryException("limit must be positive");
        CycleRecord oldest = historyQuery.oldestCycle(in.agent()).orElse(null);
        CycleRecord newest = historyQuery.lastClosedCycle(in.agent()).orElse(null);
        SearchOut.Window window = new SearchOut.Window(oldest == null ? null : oldest.cycle(), newest == null ? null : newest.cycle());
        if (newest == null) return new SearchOut(in.agent(), in.q(), window, 0, List.of());
        long cut = newest.sequenceEnd();
        String q = in.q().toLowerCase(Locale.ROOT);

        List<SearchOut.Hit> hits = new ArrayList<>();
        for (Entity<?> e : historyQuery.entitiesOfTypes(in.agent(), types)) {
            Occurrence first = firstFor(e, in.agent());
            if (first == null || first.sequence() > cut) continue;
            Occurrence end = e.getEnd() != null && e.getEnd().sequence() <= cut ? e.getEnd() : null;
            int firstCycle = first.cycle();
            Integer lastCycle = end == null ? null : end.cycle();
            if (in.to() != null && firstCycle > in.to()) continue;
            if (in.from() != null && lastCycle != null && lastCycle < in.from()) continue;
            if (!searchableText(e, cut).toLowerCase(Locale.ROOT).contains(q)) continue;
            hits.add(new SearchOut.Hit(e.typeName(), EntityIds.format(e), e.label(), firstCycle, lastCycle));
        }
        hits.sort(Comparator.comparingInt(SearchOut.Hit::firstCycle).thenComparingLong(h -> EntityIds.parse(h.entityId())));
        int total = hits.size();
        return new SearchOut(in.agent(), in.q(), window, total, HistoryView.page(hits, limit));
    }

    private static Occurrence firstFor(Entity<?> e, String agent) {
        if (e instanceof Message m && (m.getStart() == null || !m.getStart().agent().equals(agent))) return m.getArrival();
        return e.getStart();
    }

    private static String searchableText(Entity<?> e, long cut) {
        StringBuilder sb = new StringBuilder(e.label());
        if (e instanceof Belief b) {
            for (Annotation a : b.getAnnotations().elementsBefore(cut + 1)) sb.append(' ').append(a.term());
        } else if (e instanceof Event ev) {
            for (TimelineEntry<Trigger> t : ev.getTrigger().entries()) sb.append(' ').append(t.value().text());
        } else if (e instanceof Message m) {
            sb.append(' ').append(m.getSender()).append(' ').append(m.getReceiver());
        } else if (e instanceof Action a) {
            sb.append(' ').append(a.getAction());
        }
        return sb.toString();
    }
}
