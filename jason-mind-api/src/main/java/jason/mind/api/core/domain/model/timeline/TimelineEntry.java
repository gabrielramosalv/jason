package jason.mind.api.core.domain.model.timeline;

public record TimelineEntry<T>(Occurrence occurrence, T value) {
}
