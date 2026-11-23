package jason.mind.api.core.domain.model.entities;

import java.util.Map;

import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;

public final class Action extends Entity<Action> {

    public enum Result { SUCCESS, FAILURE }

    private final String action;
    private final Timeline<ActionResult> result = new Timeline<>();

    public Action(long id, Occurrence start, String action) {
        super(id, start);
        this.action = action;
    }

    public String getAction() { return action; }

    public Timeline<ActionResult> getResult() { return result; }

    @Override public Map<String, Timeline<?>> timelines() { return Map.of("result", result); }

    @Override public String typeName() { return "action"; }

    @Override public String label() { return action; }
}
