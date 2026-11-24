package jason.mind.api.core.domain.model.timeline;

/**
 * Change of a collection: an element added or removed.
 *
 * @param element   element affected by the change
 * @param operation operation applied to the element
 * @param <T>       type of the element
 * @author Gabriel Lima
 */
public record CollectionChange<T>(T element, Operation operation) {

    /**
     * Operation applied to a collection element.
     */
    public enum Operation {
        /** The element was added. */
        ADDED,
        /** The element was removed. */
        REMOVED
    }
}
