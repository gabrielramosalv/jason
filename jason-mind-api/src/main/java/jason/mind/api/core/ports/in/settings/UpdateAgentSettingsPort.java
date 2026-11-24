package jason.mind.api.core.ports.in.settings;

/**
 * Input port that sets or removes the retention of one agent.
 *
 * @author Gabriel Lima
 */
public interface UpdateAgentSettingsPort {

    /**
     * Sets the retention of an agent, or removes it when none is given. The prune applies at the end of the next
     * cycle of the agent.
     *
     * @param in agent and its retention
     * @return the settings in force after the change
     */
    SettingsOut execute(UpdateAgentSettingsIn in);
}
