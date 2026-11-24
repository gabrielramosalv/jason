package jason.mind.api.core.ports.in.capture;

import jason.mind.api.core.domain.model.timeline.Moment;

/**
 * Input of an occurrence to be recorded in the history of an agent.
 * Each implementation describes one kind of occurrence.
 *
 * @author Gabriel Lima
 */
public sealed interface RecordOccurrenceIn {

    /**
     * Returns the name of the agent.
     *
     * @return agent of the occurrence
     */
    String agent();

    /**
     * Returns the moment of the occurrence.
     *
     * @return moment of the occurrence
     */
    Moment moment();

    /**
     * Creation of an entity.
     *
     * @param agent  name of the agent
     * @param moment moment of the creation
     * @param cause  cause of the creation, or null
     * @param spec   specification of the entity
     */
    record Birth(String agent, Moment moment, CauseIn cause, EntitySpec spec) implements RecordOccurrenceIn {

    }

    /**
     * Change of a value timeline of an entity.
     *
     * @param agent    name of the agent
     * @param moment   moment of the change
     * @param cause    cause of the change, or null
     * @param entityId identifier of the entity
     * @param timeline name of the timeline
     * @param value    new value
     */
    record Change(String agent, Moment moment, CauseIn cause, long entityId, String timeline, Object value)
            implements RecordOccurrenceIn {

    }

    /**
     * Addition or removal of an element of a collection timeline of an entity.
     *
     * @param agent      name of the agent
     * @param moment     moment of the change
     * @param cause      cause of the change, or null
     * @param entityId   identifier of the entity
     * @param collection name of the collection
     * @param element    element added or removed
     * @param added      true for an addition, false for a removal
     */
    record CollectionChange(String agent, Moment moment, CauseIn cause, long entityId, String collection,
                            Object element, boolean added) implements RecordOccurrenceIn {

    }

    /**
     * Arrival of a message at its receiver.
     *
     * @param agent     name of the receiving agent
     * @param moment    moment of the arrival
     * @param messageId identifier of the message
     */
    record Arrival(String agent, Moment moment, long messageId) implements RecordOccurrenceIn {

    }

    /**
     * Variable binding added to an intended means.
     *
     * @param agent           name of the agent
     * @param moment          moment of the binding
     * @param intendedMeansId identifier of the intended means
     * @param name            name of the variable
     * @param value           bound value
     */
    record AddBinding(String agent, Moment moment, long intendedMeansId, String name, String value)
            implements RecordOccurrenceIn {

    }

    /**
     * End of an entity.
     *
     * @param agent    name of the agent
     * @param moment   moment of the end
     * @param cause    cause of the end, or null
     * @param entityId identifier of the entity
     */
    record End(String agent, Moment moment, CauseIn cause, long entityId) implements RecordOccurrenceIn {

    }
}
