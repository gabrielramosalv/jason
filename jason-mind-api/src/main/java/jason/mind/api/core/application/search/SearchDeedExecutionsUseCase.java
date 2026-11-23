package jason.mind.api.core.application.search;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import jason.mind.api.core.application.history.EntityIds;
import jason.mind.api.core.application.history.EntityNotFoundException;
import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.domain.model.entities.Plan;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.ports.in.search.SearchDeedExecutionsIn;
import jason.mind.api.core.ports.in.search.SearchDeedExecutionsPort;
import jason.mind.api.core.ports.in.search.SearchOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

public class SearchDeedExecutionsUseCase implements SearchDeedExecutionsPort {

    private final HistoryQueryRepositoryPort historyQuery;

    public SearchDeedExecutionsUseCase(HistoryQueryRepositoryPort historyQuery) {
        this.historyQuery = historyQuery;
    }

    @Override
    public SearchOut execute(SearchDeedExecutionsIn in) {
        HistoryView.agent(historyQuery, in.agent());
        long id = EntityIds.parse(in.plan());
        if (id < 0 || in.deedIndex() == null || in.deedIndex() < 0) throw new InvalidQueryException("plan and deedIndex are required");
        Entity<?> e = historyQuery.entityById(in.agent(), id).orElse(null);
        if (!(e instanceof Plan plan)) throw new EntityNotFoundException(in.agent(), in.plan());
        if (in.deedIndex() >= plan.getBody().size()) throw new InvalidQueryException("deedIndex out of the plan body");
        CycleRecord oldest = historyQuery.oldestCycle(in.agent()).orElse(null);
        CycleRecord newest = historyQuery.lastClosedCycle(in.agent()).orElse(null);
        SearchOut.Window window = new SearchOut.Window(oldest == null ? null : oldest.cycle(), newest == null ? null : newest.cycle());
        TreeSet<Integer> cycles = new TreeSet<>();
        if (newest != null)
            for (Moment m : historyQuery.deedExecutions(in.agent(), plan, in.deedIndex()))
                if (m.cycle() <= newest.cycle()) cycles.add(m.cycle());
        String label = plan.getBody().get(in.deedIndex()).content();
        List<SearchOut.Hit> hits = new ArrayList<>();
        Integer runStart = null;
        Integer previous = null;
        for (int c : cycles) {
            if (previous != null && c != previous + 1) {
                hits.add(new SearchOut.Hit("deed", null, label, runStart, previous));
                runStart = null;
            }
            if (runStart == null) runStart = c;
            previous = c;
        }
        if (runStart != null) hits.add(new SearchOut.Hit("deed", null, label, runStart, previous));
        return new SearchOut(in.agent(), null, window, hits.size(), hits);
    }
}
