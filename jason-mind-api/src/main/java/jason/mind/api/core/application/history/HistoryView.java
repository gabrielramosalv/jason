package jason.mind.api.core.application.history;

import java.util.List;

import jason.mind.api.core.domain.model.entities.Action;
import jason.mind.api.core.domain.model.entities.ActionResult;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.CollectionChange;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.ports.in.history.EntityRefOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

public final class HistoryView {

    private HistoryView() {
    }

    public static Agent agent(HistoryQueryRepositoryPort query, String name) {
        return query.agent(name).orElseThrow(() -> new AgentNotFoundException(name));
    }

    public static boolean visible(Occurrence o, long cut) {
        return o != null && o.sequence() <= cut;
    }

    public static boolean aliveAt(Entity<?> e, long cut) {
        Occurrence s = e.getStart();
        if (s == null || s.sequence() > cut) return false;
        Occurrence end = e.getEnd();
        return end == null || end.sequence() > cut;
    }

    public static long lastCut(HistoryQueryRepositoryPort query, String agent) {
        return query.lastClosedCycle(agent).map(CycleRecord::sequenceEnd).orElse(-1L);
    }

    public static EntityRefOut ref(Entity<?> e) {
        if (e == null) return null;
        return new EntityRefOut(e.typeName(), EntityIds.format(e), e.label());
    }

    public static String valueText(Object value) {
        if (value == null) return null;
        if (value instanceof Entity<?> e) return EntityIds.format(e);
        if (value instanceof Trigger t) return t.text();
        if (value instanceof CollectionChange<?> c) return c.operation().name() + " " + c.element();
        if (value instanceof ActionResult r)
            return r.result() == Action.Result.FAILURE && r.failureReason() != null ? "FAILURE " + r.failureReason() : r.result().name();
        if (value instanceof Enum<?> en) return en.name();
        return String.valueOf(value);
    }

    public static <T> List<T> page(List<T> list, int limit) {
        return list.size() <= limit ? list : list.subList(0, limit);
    }
}
