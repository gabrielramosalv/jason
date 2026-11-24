package jason.mind.api.core.ports.in.capture;

import jason.mind.api.core.domain.model.timeline.Moment;

/**
 * Input that marks the history of an agent as incomplete.
 *
 * @param name   name of the agent
 * @param moment moment from which the history is incomplete
 * @author Gabriel Lima
 */
public record MarkHistoryIncompleteIn(String name, Moment moment) {

}
