package jason.mind.api.core.domain.model.entities;

import jason.mind.api.core.domain.model.timeline.AppendOnlyList;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Intended means of an intention, that is, an instance of a plan being executed.
 * Keeps the plan, the timeline of the current deed and the variable bindings.
 *
 * @author Gabriel Lima
 */
public final class IntendedMeans extends Entity {

    /** Plan instantiated by this intended means. */
    @Getter
    private final Plan plan;

    /** Timeline of the index of the deed being executed. */
    @Getter
    private final Timeline<Integer> currentDeed = new Timeline<>();

    /** Variable bindings of the intended means. */
    private final AppendOnlyList<Binding> bindings = new AppendOnlyList<>();

    /**
     * Creates the intended means with its identifier, start occurrence and plan.
     * The current deed starts empty.
     *
     * @param id    unique identifier of the entity
     * @param start occurrence that created the intended means
     * @param plan  {@link #plan}
     */
    public IntendedMeans(long id, Occurrence start, Plan plan) {
        super(id, start);
        this.plan = plan;
        this.currentDeed.put(start, null);
    }

    /**
     * Appends a variable binding.
     *
     * @param binding binding to append
     */
    public void addBinding(Binding binding) {
        this.bindings.add(binding);
    }

    /**
     * Replaces all variable bindings with the given ones.
     *
     * @param kept bindings that remain
     */
    public void replaceBindings(List<Binding> kept) {
        this.bindings.replaceAll(kept);
    }

    /**
     * Returns a snapshot of the variable bindings.
     *
     * @return bindings in insertion order
     */
    public List<Binding> getBindings() {
        return this.bindings.snapshot();
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        return Map.of("currentDeed", this.currentDeed);
    }

    @Override
    public String typeName() {
        return "intendedMeans";
    }

    @Override
    public String label() {
        if (this.getStart().cause() != null && this.getStart().cause().by() instanceof Event e) {
            return e.getTrigger().before(this.getStart().sequence() + 1).text();
        }
        return this.plan == null ? "#" + this.getId() : this.plan.getTrigger().text();
    }
}
