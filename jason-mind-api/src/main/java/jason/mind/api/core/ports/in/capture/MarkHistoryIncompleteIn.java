package jason.mind.api.core.ports.in.capture;

import jason.mind.api.core.domain.model.timeline.Moment;

public record MarkHistoryIncompleteIn(String name, Moment moment) {
}
