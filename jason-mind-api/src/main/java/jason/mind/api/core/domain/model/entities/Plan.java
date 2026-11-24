package jason.mind.api.core.domain.model.entities;

import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Deed;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.program.SourceLocation;
import jason.mind.api.core.domain.model.program.Trigger;
import jason.mind.api.core.domain.model.timeline.CollectionTimeline;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Plan in the plan library of an agent.
 * Keeps its label, trigger, context, body, annotations, location and the timeline of its sources.
 *
 * @author Gabriel Lima
 */
@Getter
public final class Plan extends Entity {

    /** Explicit label of the plan, or null. */
    private final String label;

    /** Key that orders the plan inside the library. */
    private final long orderKey;

    /** Triggering event of the plan. */
    private final Trigger trigger;

    /** Terms of the plan context. */
    private final List<String> context;

    /** Deeds of the plan body. */
    private final List<Deed> body;

    /** Annotations of the plan label. */
    private final List<Annotation> annotations;

    /** Location of the plan in the source code. */
    private final SourceLocation source;

    /** Timeline of the sources of the plan. */
    private final CollectionTimeline<Source> sources = new CollectionTimeline<>();

    /**
     * Creates the plan with its identifier, start occurrence and definition. The lists are copied.
     *
     * @param id          unique identifier of the entity
     * @param start       occurrence that added the plan
     * @param label       {@link #label}
     * @param orderKey    {@link #orderKey}
     * @param trigger     {@link #trigger}
     * @param context     terms of the plan context, copied into {@link #context}
     * @param body        deeds of the plan body, copied into {@link #body}
     * @param annotations annotations of the label, copied into {@link #annotations}
     * @param source      {@link #source}
     */
    public Plan(long id, Occurrence start, String label, long orderKey, Trigger trigger, List<String> context,
                List<Deed> body, List<Annotation> annotations, SourceLocation source) {
        super(id, start);
        this.label = label;
        this.orderKey = orderKey;
        this.trigger = trigger;
        this.context = List.copyOf(context);
        this.body = List.copyOf(body);
        this.annotations = List.copyOf(annotations);
        this.source = source;
    }

    @Override
    public Map<String, CollectionTimeline<?>> collections() {
        return Map.of("sources", this.sources);
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        return Map.of();
    }

    @Override
    public String typeName() {
        return "plan";
    }

    @Override
    public String label() {
        return this.label == null ? this.trigger.text() : this.label;
    }
}
