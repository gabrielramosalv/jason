package jason.mind.api.core.domain.model.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jason.mind.api.core.domain.model.program.Annotation;
import jason.mind.api.core.domain.model.program.Source;
import jason.mind.api.core.domain.model.timeline.CollectionTimeline;
import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.Timeline;

public final class Belief extends Entity<Belief> {

    private final String content;
    private final boolean rule;
    private final long orderKey;
    private final CollectionTimeline<Annotation> annotations = new CollectionTimeline<>();

    public Belief(long id, Occurrence start, String content, boolean rule, long orderKey) {
        super(id, start);
        this.content = content;
        this.rule = rule;
        this.orderKey = orderKey;
    }

    public String getContent() { return content; }

    public boolean isRule() { return rule; }

    public long getOrderKey() { return orderKey; }

    public CollectionTimeline<Annotation> getAnnotations() { return annotations; }

    public static List<Source> sourcesOf(Set<Annotation> annotations) {
        List<Source> r = new ArrayList<>();
        for (Annotation a : annotations) {
            Source s = Source.of(a);
            if (s != null) r.add(s);
        }
        return r;
    }

    public List<Source> getSources() {
        return sourcesOf(annotations.current());
    }

    @Override public Map<String, Timeline<?>> timelines() { return Map.of(); }

    @Override public Map<String, CollectionTimeline<?>> collections() { return Map.of("annotations", annotations); }

    @Override public String typeName() { return "belief"; }

    @Override public String label() { return content; }
}
