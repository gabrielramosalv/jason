package jason.mind.api.infrastructure.adapters.in.jason.capture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe directory of recorded message entities.
 * <p>
 * Maps a Jason message id and its receiver to the id of the message entity recorded for them, so that the sender
 * and the receiver of a message resolve to the same entity.
 *
 * @author Gabriel Lima
 */
final class MessageDirectory {

    /** Recorded entity ids indexed by the combined message id and receiver key. */
    private final Map<String, Long> ids = new ConcurrentHashMap<>();

    /** Combined key of each recorded entity id, to drop an entry knowing only the entity. */
    private final Map<Long, String> keys = new ConcurrentHashMap<>();

    /**
     * Builds the lookup key of a message.
     *
     * @param msgId    Jason id of the message
     * @param receiver name of the agent that receives the message
     * @return the message id and receiver joined by a NUL character
     */
    private static String key(String msgId, String receiver) {
        return msgId + '\u0000' + receiver;
    }

    /**
     * Stores the entity id of a message; does nothing when the message id or the receiver is {@code null}.
     *
     * @param msgId    Jason id of the message
     * @param receiver name of the agent that receives the message
     * @param id       id of the recorded message entity
     */
    void put(String msgId, String receiver, long id) {
        if (msgId != null && receiver != null) {
            String key = key(msgId, receiver);
            this.ids.put(key, id);
            this.keys.put(id, key);
        }
    }

    /**
     * Drops the entry of a message entity that was pruned from the history; does nothing when it has none.
     *
     * @param id id of the pruned message entity
     */
    void forget(long id) {
        String key = this.keys.remove(id);
        if (key != null) {
            this.ids.remove(key, id);
        }
    }

    /**
     * Looks up the entity id of a message.
     *
     * @param msgId    Jason id of the message
     * @param receiver name of the agent that receives the message
     * @return the recorded entity id, or {@code null} when unknown or when any argument is {@code null}
     */
    Long get(String msgId, String receiver) {
        if (msgId == null || receiver == null) {
            return null;
        }
        return this.ids.get(key(msgId, receiver));
    }
}
