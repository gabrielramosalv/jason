package jason.mind.api.core.ports.out.sequence;

/**
 * Port for generating entity identifiers.
 *
 * @author Gabriel Lima
 */
public interface EntityIdProviderPort {

    /**
     * Generates the next entity identifier.
     *
     * @return new identifier
     */
    long next();
}
