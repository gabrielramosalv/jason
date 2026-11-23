package jason.mind.api.core.domain.model.entities;

import java.util.List;
import java.util.Map;

import jason.mind.api.core.domain.model.timeline.AppendOnlyList;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;

public final class IntendedMeans extends Entity<IntendedMeans> {

    private final Plan plan;
    private final Timeline<Integer> currentDeed = new Timeline<>();
    private final AppendOnlyList<Binding> bindings = new AppendOnlyList<>();

    public IntendedMeans(long id, Occurrence start, Plan plan) {
        super(id, start);
        this.plan = plan;
        this.currentDeed.put(start, null);
    }

    public Plan getPlan() { return plan; }

    public Timeline<Integer> getCurrentDeed() { return currentDeed; }

    public List<Binding> getBindings() { return bindings.snapshot(); }

    public void addBinding(Binding binding) { bindings.add(binding); }

    public void replaceBindings(List<Binding> kept) { bindings.replaceAll(kept); }

    @Override public Map<String, Timeline<?>> timelines() { return Map.of("currentDeed", currentDeed); }

    @Override public String typeName() { return "intendedMeans"; }

    @Override
    public String label() {
        if (getStart().cause() != null && getStart().cause().by() instanceof Event e)
            return e.getTrigger().before(getStart().sequence() + 1).text();
        return plan == null ? "#" + getId() : plan.getTrigger().text();
    }
}
