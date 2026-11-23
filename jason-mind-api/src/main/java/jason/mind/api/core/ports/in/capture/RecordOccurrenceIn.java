package jason.mind.api.core.ports.in.capture;

import jason.mind.api.core.domain.model.timeline.Moment;

public sealed interface RecordOccurrenceIn {

    String agent();

    Moment moment();

    record Birth(String agent, Moment moment, CauseIn cause, EntitySpec spec) implements RecordOccurrenceIn {
    }

    record Change(String agent, Moment moment, CauseIn cause, long entityId, String timeline, Object value) implements RecordOccurrenceIn {
    }

    record CollectionChange(String agent, Moment moment, CauseIn cause, long entityId, String collection, Object element,
                            boolean added) implements RecordOccurrenceIn {
    }

    record Arrival(String agent, Moment moment, long messageId) implements RecordOccurrenceIn {
    }

    record AddBinding(String agent, Moment moment, long intendedMeansId, String name, String value) implements RecordOccurrenceIn {
    }

    record End(String agent, Moment moment, CauseIn cause, long entityId) implements RecordOccurrenceIn {
    }
}
