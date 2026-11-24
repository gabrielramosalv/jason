package jason.mind.api.core.ports.in.settings;

/**
 * Input of the change of the retention of one agent.
 *
 * @param agent           name of the agent
 * @param retentionCycles cycles the agent retains, or null to go back to the default of the system
 * @author Gabriel Lima
 */
public record UpdateAgentSettingsIn(String agent, Long retentionCycles) {

}
