package jason.mind.api.core.domain.model.entities;

import java.util.List;
import java.util.Map;

import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Deed;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.program.SourceLocation;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.CollectionTimeline;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;

public final class Plan extends Entity<Plan> {

    private final String planLabel;
    private final long orderKey;
    private final Trigger trigger;
    private final List<String> context;
    private final List<Deed> body;
    private final List<Annotation> annotations;
    private final SourceLocation source;
    private final CollectionTimeline<Source> sources = new CollectionTimeline<>();

    public Plan(long id, Occurrence start, String label, long orderKey, Trigger trigger, List<String> context,
                List<Deed> body, List<Annotation> annotations, SourceLocation source) {
        super(id, start);
        this.planLabel = label;
        this.orderKey = orderKey;
        this.trigger = trigger;
        this.context = List.copyOf(context);
        this.body = List.copyOf(body);
        this.annotations = List.copyOf(annotations);
        this.source = source;
    }

    public String getLabel() { return planLabel; }

    public long getOrderKey() { return orderKey; }

    public Trigger getTrigger() { return trigger; }

    public List<String> getContext() { return context; }

    public List<Deed> getBody() { return body; }

    public List<Annotation> getAnnotations() { return annotations; }

    public SourceLocation getSource() { return source; }

    public CollectionTimeline<Source> getSources() { return sources; }

    @Override public Map<String, Timeline<?>> timelines() { return Map.of(); }

    @Override public Map<String, CollectionTimeline<?>> collections() { return Map.of("sources", sources); }

    @Override public String typeName() { return "plan"; }

    @Override public String label() { return planLabel == null ? trigger.text() : planLabel; }
}
