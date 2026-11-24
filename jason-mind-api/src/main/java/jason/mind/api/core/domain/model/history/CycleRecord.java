package jason.mind.api.core.domain.model.history;

/**
 * Record of a finished reasoning cycle.
 * Holds when it ended and which phases ran in it.
 *
 * @param cycle               number of the cycle
 * @param timeEndMilliseconds wall-clock time of the end of the cycle, in milliseconds
 * @param sequenceEnd         last global sequence number of the cycle
 * @param sense               whether the sense phase ran
 * @param deliberate          whether the deliberate phase ran
 * @param act                 whether the act phase ran
 * @author Gabriel Lima
 */
public record CycleRecord(int cycle, long timeEndMilliseconds, long sequenceEnd, boolean sense, boolean deliberate,
                          boolean act) {

}
