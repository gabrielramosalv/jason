package jason.mind.api.core.domain.model.entities;

import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.timeline.CollectionTimeline;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Belief or rule held in the belief base of an agent.
 * Keeps its content, its position in the base and the timeline of its annotations.
 *
 * @author Gabriel Lima
 */
@Getter
public final class Belief extends Entity {

    /** Text of the belief or rule. */
    private final String content;

    /** Whether this entry is a rule instead of a fact. */
    private final boolean rule;

    /** Key that orders the entry inside the belief base. */
    private final long orderKey;

    /** Timeline of the annotations attached to the belief. */
    private final CollectionTimeline<Annotation> annotations = new CollectionTimeline<>();

    /**
     * Creates the belief with its identifier, start occurrence, content, kind and order.
     *
     * @param id       unique identifier of the entity
     * @param start    occurrence that added the belief
     * @param content  {@link #content}
     * @param rule     {@link #rule}
     * @param orderKey {@link #orderKey}
     */
    public Belief(long id, Occurrence start, String content, boolean rule, long orderKey) {
        super(id, start);
        this.content = content;
        this.rule = rule;
        this.orderKey = orderKey;
    }

    /**
     * Extracts the sources from a set of annotations, ignoring the annotations that are not sources.
     *
     * @param annotations annotations to inspect
     * @return sources found, in the iteration order of the set
     */
    public static List<Source> sourcesOf(Set<Annotation> annotations) {
        List<Source> r = new ArrayList<>();
        for (Annotation a : annotations) {
            Source s = Source.of(a);
            if (s != null) {
                r.add(s);
            }
        }
        return r;
    }

    /**
     * Returns the sources currently annotated on the belief.
     *
     * @return sources of the current annotations
     */
    public List<Source> getSources() {
        return sourcesOf(this.annotations.current());
    }

    @Override
    public Map<String, CollectionTimeline<?>> collections() {
        return Map.of("annotations", this.annotations);
    }

    @Override
    public Map<String, Timeline<?>> timelines() {
        return Map.of();
    }

    @Override
    public String typeName() {
        return "belief";
    }

    @Override
    public String label() {
        return this.content;
    }
}
