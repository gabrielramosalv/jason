package jason.mind.api.core.application.cycle;

import java.util.ArrayList;
import java.util.List;

import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.ports.in.cycle.CyclesOut;
import jason.mind.api.core.ports.in.cycle.ListCyclesIn;
import jason.mind.api.core.ports.in.cycle.ListCyclesPort;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

public class ListCyclesUseCase implements ListCyclesPort {

    private final HistoryQueryRepositoryPort historyQuery;

    public ListCyclesUseCase(HistoryQueryRepositoryPort historyQuery) {
        this.historyQuery = historyQuery;
    }

    @Override
    public CyclesOut execute(ListCyclesIn in) {
        HistoryView.agent(historyQuery, in.agent());
        CycleRecord oldest = historyQuery.oldestCycle(in.agent()).orElse(null);
        CycleRecord newest = historyQuery.lastClosedCycle(in.agent()).orElse(null);
        if (oldest == null || newest == null) return new CyclesOut(in.agent(), null, null, null, null, List.of());
        int from = clamp(in.from() == null ? oldest.cycle() : in.from(), oldest.cycle(), newest.cycle());
        int to = clamp(in.to() == null ? newest.cycle() : in.to(), oldest.cycle(), newest.cycle());
        List<CyclesOut.CycleItem> items = new ArrayList<>();
        if (from <= to) {
            List<CycleRecord> records = historyQuery.cycles(in.agent(), from, to);
            int k = 0;
            for (int c = from; c <= to; c++) {
                while (k < records.size() && records.get(k).cycle() < c) k++;
                CycleRecord r = k < records.size() && records.get(k).cycle() == c ? records.get(k) : null;
                items.add(new CyclesOut.CycleItem(c, r == null ? new CyclesOut.Phases(false, false, false)
                        : new CyclesOut.Phases(r.sense(), r.deliberate(), r.act())));
            }
        }
        return new CyclesOut(in.agent(), oldest.cycle(), newest.cycle(), from, to, items);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
