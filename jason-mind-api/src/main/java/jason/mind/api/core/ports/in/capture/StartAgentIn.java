package jason.mind.api.core.ports.in.capture;

import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.timeline.Moment;

public record StartAgentIn(String name, AgentConfig config, Moment parentMoment, CauseIn parentCause) {
}
