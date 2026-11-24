package jason.mind.api.core.ports.out.mas;

/**
 * Port for storing the name of the multi-agent system.
 *
 * @author Gabriel Lima
 */
public interface MasRepositoryPort {

    /**
     * Saves the name of the system.
     *
     * @param name name of the system
     */
    void save(String name);

    /**
     * Returns the name of the system.
     *
     * @return saved name
     */
    String name();
}
