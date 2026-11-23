package jason.mind.api.core.domain.model.timeline;

public record CollectionChange<T>(T element, Operation operation) {
    public enum Operation { ADDED, REMOVED }
}
