package jason.mind.api.core.domain.model.entities;

import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;
import lombok.Getter;

import java.util.Map;

/**
 * Environment action executed by an agent.
 * Keeps the action text and the timeline of its result.
 *
 * @author Gabriel Lima
 */
@Getter
public final class Action extends Entity {

    /** Text of the action. */
    private final String action;

    /** Timeline of the action result. */
    private final Timeline<ActionResult> result = new Timeline<>();

    /**
     * Creates the action with its identifier, start occurrence and text.
     *
     * @param id     unique identifier of the entity
     * @param start  occurrence that started the action
     * @param action {@link #action}
     */
    public Action(long id, Occurrence start, String action) {
        super(id, start);
        this.action = action;
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        return Map.of("result", this.result);
    }

    @Override
    public String typeName() {
        return "action";
    }

    @Override
    public String label() {
        return this.action;
    }

    /**
     * Outcome of an executed action.
     * Distinguishes a successful action from a failed one.
     */
    public enum Result {
        /** The action succeeded. */
        SUCCESS,
        /** The action failed. */
        FAILURE
    }
}
