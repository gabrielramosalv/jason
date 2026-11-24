package jason.mind.api.core.ports.in.mas;

/**
 * Port for querying the multi-agent system and its agents.
 *
 * @author Gabriel Lima
 */
public interface FindMasPort {

    /**
     * Finds the multi-agent system.
     *
     * @return name, versions and agents of the system
     */
    MasOut execute();
}
