package jason.mind.api.core.application.cycle;

import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.ports.in.cycle.CyclesOut;
import jason.mind.api.core.ports.in.cycle.ListCyclesIn;
import jason.mind.api.core.ports.in.cycle.ListCyclesPort;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case that lists the retained cycles of an agent within a range.
 * The range is clamped to the retained window and every cycle in it is listed with the phases that ran;
 * cycles without a record are listed with no phase.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class ListCyclesUseCase implements ListCyclesPort {

    /** Read-only access to the cycle records. */
    private final HistoryQueryRepositoryPort historyQuery;

    /**
     * Limits a value to an inclusive range.
     *
     * @param v   value to limit
     * @param min lowest accepted value
     * @param max highest accepted value
     * @return the value moved into the range
     */
    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public CyclesOut execute(ListCyclesIn in) {
        HistoryView.agent(this.historyQuery, in.agent());
        CycleRecord oldest = this.historyQuery.oldestCycle(in.agent()).orElse(null);
        CycleRecord newest = this.historyQuery.lastClosedCycle(in.agent()).orElse(null);
        if (oldest == null || newest == null) {
            return new CyclesOut(in.agent(), null, null, null, null, List.of());
        }
        int from = clamp(in.from() == null ? oldest.cycle() : in.from(), oldest.cycle(), newest.cycle());
        int to = clamp(in.to() == null ? newest.cycle() : in.to(), oldest.cycle(), newest.cycle());
        List<CyclesOut.CycleItem> items = new ArrayList<>();
        if (from <= to) {
            List<CycleRecord> records = this.historyQuery.cycles(in.agent(), from, to);
            int k = 0;
            for (int c = from; c <= to; c++) {
                while (k < records.size() && records.get(k).cycle() < c) {
                    k++;
                }
                CycleRecord r = k < records.size() && records.get(k).cycle() == c ? records.get(k) : null;
                items.add(new CyclesOut.CycleItem(c, r == null ? new CyclesOut.Phases(false, false, false) :
                                                     new CyclesOut.Phases(r.sense(), r.deliberate(), r.act())));
            }
        }
        return new CyclesOut(in.agent(), oldest.cycle(), newest.cycle(), from, to, items);
    }
}
