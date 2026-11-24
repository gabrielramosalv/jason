package jason.mind.api.core.ports.in.plan;

/**
 * Port for querying the plans of an agent.
 *
 * @author Gabriel Lima
 */
public interface FindPlansPort {

    /**
     * Finds the plans of an agent.
     *
     * @param in agent
     * @return plans of the agent
     */
    PlansOut execute(FindPlansIn in);
}
