package jason.mind.api.core.ports.in.capture;

/**
 * Input of the end of an agent.
 *
 * @param name      name of the agent
 * @param lastCycle last cycle the agent ran
 * @author Gabriel Lima
 */
public record EndAgentIn(String name, int lastCycle) {

}
