package jason.mind.api.core.ports.out.sequence;

/**
 * Port for generating occurrence sequence numbers per agent.
 *
 * @author Gabriel Lima
 */
public interface SequenceProviderPort {

    /**
     * Generates the next sequence number of an agent.
     *
     * @param agent name of the agent
     * @return new sequence number
     */
    long next(String agent);
}
