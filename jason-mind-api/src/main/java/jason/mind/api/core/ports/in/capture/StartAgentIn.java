package jason.mind.api.core.ports.in.capture;

import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.timeline.Moment;

/**
 * Input of the start of an agent.
 *
 * @param name         name of the agent
 * @param config       configuration of the agent
 * @param parentMoment moment in the creating agent, or null
 * @param parentCause  cause in the creating agent, or null
 * @author Gabriel Lima
 */
public record StartAgentIn(String name, AgentConfig config, Moment parentMoment, CauseIn parentCause) {

}
