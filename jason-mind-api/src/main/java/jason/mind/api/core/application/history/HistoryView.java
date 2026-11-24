package jason.mind.api.core.application.history;

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

import java.util.List;

/**
 * Utility with the helpers shared by the query use cases to read the history.
 * It finds agents, checks visibility against a sequence cut, and formats entities and values for output.
 *
 * @author Gabriel Lima
 */
public final class HistoryView {

    /**
     * Prevents instantiation of this utility class.
     */
    private HistoryView() {
    }

    /**
     * Finds an agent in the history.
     *
     * @param query read-only access to the history
     * @param name  name of the agent
     * @return the agent
     * @throws AgentNotFoundException if the agent is not in the history
     */
    public static Agent agent(HistoryQueryRepositoryPort query, String name) {
        return query.agent(name).orElseThrow(() -> new AgentNotFoundException(name));
    }

    /**
     * Tells whether an occurrence happened up to a sequence cut.
     *
     * @param o   occurrence to check, possibly {@code null}
     * @param cut last visible sequence
     * @return {@code true} if the occurrence exists and is not after the cut
     */
    public static boolean isVisible(Occurrence o, long cut) {
        return o != null && o.sequence() <= cut;
    }

    /**
     * Tells whether an entity is alive at a sequence cut, meaning it started up to the cut and did not end by then.
     *
     * @param e   entity to check
     * @param cut sequence at which the entity is observed
     * @return {@code true} if the entity is alive at the cut
     */
    public static boolean aliveAt(Entity e, long cut) {
        Occurrence s = e.getStart();
        if (s == null || s.sequence() > cut) {
            return false;
        }
        Occurrence end = e.getEnd();
        return end == null || end.sequence() > cut;
    }

    /**
     * Returns the closing sequence of the last closed cycle of an agent.
     *
     * @param query read-only access to the history
     * @param agent name of the agent
     * @return the closing sequence, or {@code -1} when no cycle was closed
     */
    public static long lastCut(HistoryQueryRepositoryPort query, String agent) {
        return query.lastClosedCycle(agent).map(CycleRecord::sequenceEnd).orElse(-1L);
    }

    /**
     * Builds the output reference of an entity.
     *
     * @param e entity to reference, possibly {@code null}
     * @return the reference with type, public id and label, or {@code null} when the entity is {@code null}
     */
    public static EntityRefOut ref(Entity e) {
        if (e == null) {
            return null;
        }
        return new EntityRefOut(e.typeName(), EntityIds.format(e), e.label());
    }

    /**
     * Formats a timeline value as text: entities by public id, triggers by text, collection changes as
     * operation and element, failed action results with their reason, enums by name and anything else by
     * {@link String#valueOf(Object)}.
     *
     * @param value value to format, possibly {@code null}
     * @return the text of the value, or {@code null} when the value is {@code null}
     */
    public static String formatValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Entity e) {
            return EntityIds.format(e);
        }
        if (value instanceof Trigger t) {
            return t.text();
        }
        if (value instanceof CollectionChange<?>(Object element, CollectionChange.Operation operation)) {
            return operation.name() + " " + element;
        }
        if (value instanceof ActionResult r) {
            return r.result() == Action.Result.FAILURE && r.failureReason() != null ? "FAILURE " + r.failureReason() :
                   r.result().name();
        }
        if (value instanceof Enum<?> en) {
            return en.name();
        }
        return String.valueOf(value);
    }

    /**
     * Keeps at most a given number of elements of a list.
     *
     * @param list  list to cut
     * @param limit maximum number of elements
     * @param <T>   type of the elements
     * @return the list itself when it fits, otherwise a view of its first elements
     */
    public static <T> List<T> paginate(List<T> list, int limit) {
        return list.size() <= limit ? list : list.subList(0, limit);
    }
}
