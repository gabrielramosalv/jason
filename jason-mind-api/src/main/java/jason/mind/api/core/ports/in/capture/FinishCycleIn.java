package jason.mind.api.core.ports.in.capture;

/**
 * Input of the end of a reasoning cycle.
 *
 * @param agent      name of the agent
 * @param cycle      number of the finished cycle
 * @param sense      whether the sense phase ran
 * @param deliberate whether the deliberate phase ran
 * @param act        whether the act phase ran
 * @author Gabriel Lima
 */
public record FinishCycleIn(String agent, int cycle, boolean sense, boolean deliberate, boolean act) {

}
